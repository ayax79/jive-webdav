package com.syncapse.jive.webdav.methods

import java.io.IOException
import java.util.Hashtable
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import net.sf.webdav.ITransaction
import net.sf.webdav.IWebdavStore
import net.sf.webdav.StoredObject
import net.sf.webdav.WebdavStatus
import net.sf.webdav.exceptions.AccessDeniedException
import net.sf.webdav.exceptions.LockFailedException
import net.sf.webdav.exceptions.ObjectAlreadyExistsException
import net.sf.webdav.exceptions.ObjectNotFoundException
import net.sf.webdav.exceptions.WebdavException
import net.sf.webdav.fromcatalina.RequestUtil
import net.sf.webdav.locking.ResourceLocks
import com.syncapse.jive.webdav.WebdavStatus


object DoCopy {
  private var LOG: Logger = org.slf4j.LoggerFactory.getLogger(classOf[DoCopy])
}
class DoCopy extends AbstractMethod {
  /**
   * Parses and normalizes the destination header.
   *
   * @param req
   *      Servlet request
   * @param resp
   *      Servlet response
   * @return destinationPath
   * @throws IOException
   *      if an error occurs while sending response
   */
  private def parseDestinationHeader(req: HttpServletRequest, resp: HttpServletResponse): Nothing = {
    var destinationPath: Nothing = req.getHeader("Destination")
    if (destinationPath == null) {
      resp.sendError(WebdavStatus.SC_BAD_REQUEST)
      return null
    }
    destinationPath = RequestUtil.URLDecode(destinationPath, "UTF8")
    var protocolIndex: Int = destinationPath.indexOf("://")
    if (protocolIndex >= 0) {
      var firstSeparator: Int = destinationPath.indexOf("/", protocolIndex + 4)
      if (firstSeparator < 0) {
        destinationPath = "/"
      }
      else {
        destinationPath = destinationPath.substring(firstSeparator)
      }
    }
    else {
      var hostName: Nothing = req.getServerName
      if ((hostName != null) && (destinationPath.startsWith(hostName))) {
        destinationPath = destinationPath.substring(hostName.length)
      }
      var portIndex: Int = destinationPath.indexOf(":")
      if (portIndex >= 0) {
        destinationPath = destinationPath.substring(portIndex)
      }
      if (destinationPath.startsWith(":")) {
        var firstSeparator: Int = destinationPath.indexOf("/")
        if (firstSeparator < 0) {
          destinationPath = "/"
        }
        else {
          destinationPath = destinationPath.substring(firstSeparator)
        }
      }
    }
    destinationPath = normalize(destinationPath)
    var contextPath: Nothing = req.getContextPath
    if ((contextPath != null) && (destinationPath.startsWith(contextPath))) {
      destinationPath = destinationPath.substring(contextPath.length)
    }
    var pathInfo: Nothing = req.getPathInfo
    if (pathInfo != null) {
      var servletPath: Nothing = req.getServletPath
      if ((servletPath != null) && (destinationPath.startsWith(servletPath))) {
        destinationPath = destinationPath.substring(servletPath.length)
      }
    }
    return destinationPath
  }

  private var _resourceLocks: Nothing = null

  /**
   * Copy a resource.
   *
   * @param transaction
   *      indicates that the method is within the scope of a WebDAV
   *      transaction
   * @param req
   *      Servlet request
   * @param resp
   *      Servlet response
   * @return true if the copy is successful
   * @throws WebdavException
   *      if an error in the underlying store occurs
   * @throws IOException
   *      when an error occurs while sending the response
   * @throws LockFailedException
   */
  def copyResource(transaction: Nothing, req: HttpServletRequest, resp: HttpServletResponse): Boolean = {
    var destinationPath: Nothing = parseDestinationHeader(req, resp)
    if (destinationPath == null) return false
    var path: Nothing = getRelativePath(req)
    if (path.equals(destinationPath)) {
      resp.sendError(WebdavStatus.SC_FORBIDDEN)
      return false
    }
    var errorList: Hashtable[Nothing, Nothing] = new Hashtable[Nothing, Nothing]
    var parentDestinationPath: Nothing = getParentPath(getCleanPath(destinationPath))
    if (!checkLocks(transaction, req, resp, _resourceLocks, parentDestinationPath)) {
      errorList.put(parentDestinationPath, WebdavStatus.SC_LOCKED)
      sendReport(req, resp, errorList)
      return false
    }
    if (!checkLocks(transaction, req, resp, _resourceLocks, destinationPath)) {
      errorList.put(destinationPath, WebdavStatus.SC_LOCKED)
      sendReport(req, resp, errorList)
      return false
    }
    var overwrite: Boolean = true
    var overwriteHeader: Nothing = req.getHeader("Overwrite")
    if (overwriteHeader != null) {
      overwrite = overwriteHeader.equalsIgnoreCase("T")
    }
    var lockOwner: Nothing = "copyResource" + System.currentTimeMillis + req.toString
    if (_resourceLocks.lock(transaction, destinationPath, lockOwner, false, 0, TEMP_TIMEOUT, TEMPORARY)) {
      var copySo: Nothing = null
      var destinationSo: Nothing = null
      try {
        copySo = _store.getStoredObject(transaction, path)
        if (copySo == null) {
          resp.sendError(HttpServletResponse.SC_NOT_FOUND)
          return false
        }
        if (copySo.isNullResource) {
          var methodsAllowed: Nothing = DeterminableMethod.determineMethodsAllowed(copySo)
          resp.addHeader("Allow", methodsAllowed)
          resp.sendError(WebdavStatus.SC_METHOD_NOT_ALLOWED)
          return false
        }
        errorList = new Hashtable[Nothing, Nothing]
        destinationSo = _store.getStoredObject(transaction, destinationPath)
        if (overwrite) {
          if (destinationSo != null) {
            _doDelete.deleteResource(transaction, destinationPath, errorList, req, resp)
          }
          else {
            resp.setStatus(WebdavStatus.SC_CREATED)
          }
        }
        else {
          if (destinationSo != null) {
            resp.sendError(WebdavStatus.SC_PRECONDITION_FAILED)
            return false
          }
          else {
            resp.setStatus(WebdavStatus.SC_CREATED)
          }
        }
        copy(transaction, path, destinationPath, errorList, req, resp)
        if (!errorList.isEmpty) {
          sendReport(req, resp, errorList)
        }
      }
      finally {
        _resourceLocks.unlockTemporaryLockedObjects(transaction, destinationPath, lockOwner)
      }
    }
    else {
      resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR)
      return false
    }
    return true
  }

  private var _readOnly: Boolean = false

  def this(store: Nothing, resourceLocks: Nothing, doDelete: Nothing, readOnly: Boolean) {
    this ()
    _store = store
    _resourceLocks = resourceLocks
    _doDelete = doDelete
    _readOnly = readOnly
  }

  /**
   * helper method of copy() recursively copies the FOLDER at source path to
   * destination path
   *
   * @param transaction
   *      indicates that the method is within the scope of a WebDAV
   *      transaction
   * @param sourcePath
   *      where to read
   * @param destinationPath
   *      where to write
   * @param errorList
   *      all errors that ocurred
   * @param req
   *      HttpServletRequest
   * @param resp
   *      HttpServletResponse
   * @throws WebdavException
   *      if an error in the underlying store occurs
   */
  private def copyFolder(transaction: Nothing, sourcePath: Nothing, destinationPath: Nothing, errorList: Hashtable[Nothing, Nothing], req: HttpServletRequest, resp: HttpServletResponse): Unit = {
    _store.createFolder(transaction, destinationPath)
    var infiniteDepth: Boolean = true
    var depth: Nothing = req.getHeader("Depth")
    if (depth != null) {
      if (depth.equals("0")) {
        infiniteDepth = false
      }
    }
    if (infiniteDepth) {
      var children: Array[Nothing] = _store.getChildrenNames(transaction, sourcePath)
      children = if (children == null) Array[Nothing]() else children
      var childSo: Nothing = null

      {
        var i: Int = children.length - 1
        while (i >= 0) {
          {
            children(i) = "/" + children(i)
            try {
              childSo = _store.getStoredObject(transaction, (sourcePath + children(i)))
              if (childSo.isResource) {
                _store.createResource(transaction, destinationPath + children(i))
                var resourceLength: Long = _store.setResourceContent(transaction, destinationPath + children(i), _store.getResourceContent(transaction, sourcePath + children(i)), null, null)
                if (resourceLength != -1) {
                  var destinationSo: Nothing = _store.getStoredObject(transaction, destinationPath + children(i))
                  destinationSo.setResourceLength(resourceLength)
                }
              }
              else {
                copyFolder(transaction, sourcePath + children(i), destinationPath + children(i), errorList, req, resp)
              }
            }
            catch {
              case e: Nothing => {
                errorList.put(destinationPath + children(i), new Nothing(WebdavStatus.SC_FORBIDDEN))
              }
              case e: Nothing => {
                errorList.put(destinationPath + children(i), new Nothing(WebdavStatus.SC_NOT_FOUND))
              }
              case e: Nothing => {
                errorList.put(destinationPath + children(i), new Nothing(WebdavStatus.SC_CONFLICT))
              }
              case e: Nothing => {
                errorList.put(destinationPath + children(i), new Nothing(WebdavStatus.SC_INTERNAL_SERVER_ERROR))
              }
            }
          }
          ({i -= 1; i})
        }
      }
    }
  }

  def execute(transaction: Nothing, req: HttpServletRequest, resp: HttpServletResponse): Unit = {
    LOG.trace("-- " + this.getClass.getName)
    var path: Nothing = getRelativePath(req)
    if (!_readOnly) {
      var tempLockOwner: Nothing = "doCopy" + System.currentTimeMillis + req.toString
      if (_resourceLocks.lock(transaction, path, tempLockOwner, false, 0, TEMP_TIMEOUT, TEMPORARY)) {
        try {
          if (!copyResource(transaction, req, resp)) return
        }
        catch {
          case e: Nothing => {
            resp.sendError(WebdavStatus.SC_FORBIDDEN)
          }
          case e: Nothing => {
            resp.sendError(WebdavStatus.SC_CONFLICT, req.getRequestURI)
          }
          case e: Nothing => {
            resp.sendError(WebdavStatus.SC_NOT_FOUND, req.getRequestURI)
          }
          case e: Nothing => {
            resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR)
          }
        }
        finally {
          _resourceLocks.unlockTemporaryLockedObjects(transaction, path, tempLockOwner)
        }
      }
      else {
        resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR)
      }
    }
    else {
      resp.sendError(WebdavStatus.SC_FORBIDDEN)
    }
  }

  /**
   * copies the specified resource(s) to the specified destination.
   * preconditions must be handled by the caller. Standard status codes must
   * be handled by the caller. a multi status report in case of errors is
   * created here.
   *
   * @param transaction
   *      indicates that the method is within the scope of a WebDAV
   *      transaction
   * @param sourcePath
   *      path from where to read
   * @param destinationPath
   *      path where to write
   * @param req
   *      HttpServletRequest
   * @param resp
   *      HttpServletResponse
   * @throws WebdavException
   *      if an error in the underlying store occurs
   * @throws IOException
   */
  private def copy(transaction: Nothing, sourcePath: Nothing, destinationPath: Nothing, errorList: Hashtable[Nothing, Nothing], req: HttpServletRequest, resp: HttpServletResponse): Unit = {
    var sourceSo: Nothing = _store.getStoredObject(transaction, sourcePath)
    if (sourceSo.isResource) {
      _store.createResource(transaction, destinationPath)
      var resourceLength: Long = _store.setResourceContent(transaction, destinationPath, _store.getResourceContent(transaction, sourcePath), null, null)
      if (resourceLength != -1) {
        var destinationSo: Nothing = _store.getStoredObject(transaction, destinationPath)
        destinationSo.setResourceLength(resourceLength)
      }
    }
    else {
      if (sourceSo.isFolder) {
        copyFolder(transaction, sourcePath, destinationPath, errorList, req, resp)
      }
      else {
        resp.sendError(WebdavStatus.SC_NOT_FOUND)
      }
    }
  }

  private var _doDelete: Nothing = null
  private var _store: Nothing = null

  /**
   * Return a context-relative path, beginning with a "/", that represents the
   * canonical version of the specified path after ".." and "." elements are
   * resolved out. If the specified path attempts to go outside the boundaries
   * of the current context (i.e. too many ".." path elements are present),
   * return <code>null</code> instead.
   *
   * @param path
   *      Path to be normalized
   * @return normalized path
   */
  protected def normalize(path: Nothing): Nothing = {
    if (path == null) return null
    var normalized: Nothing = path
    if (normalized.equals("/.")) return "/"
    if (normalized.indexOf('\\') >= 0) normalized = normalized.replace('\\', '/')
    if (!normalized.startsWith("/")) normalized = "/" + normalized
    while (true) {
      var index: Int = normalized.indexOf("//")
      if (index < 0) break //todo: break is not supported
      normalized = normalized.substring(0, index) + normalized.substring(index + 1)
    }
    while (true) {
      var index: Int = normalized.indexOf("/./")
      if (index < 0) break //todo: break is not supported
      normalized = normalized.substring(0, index) + normalized.substring(index + 2)
    }
    while (true) {
      var index: Int = normalized.indexOf("/../")
      if (index < 0) break //todo: break is not supported
      if (index == 0) return (null)
      var index2: Int = normalized.lastIndexOf('/', index - 1)
      normalized = normalized.substring(0, index2) + normalized.substring(index + 3)
    }
    return (normalized)
  }
}