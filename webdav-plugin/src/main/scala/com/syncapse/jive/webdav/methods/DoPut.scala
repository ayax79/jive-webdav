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
import net.sf.webdav.exceptions.WebdavException
import net.sf.webdav.locking.IResourceLocks
import net.sf.webdav.locking.LockedObject
import org.slf4j.Logger
import com.syncapse.jive.webdav.WebdavStatus


object DoPut {
  private var LOG: Logger = org.slf4j.LoggerFactory.getLogger(classOf[DoPut])
}
class DoPut extends AbstractMethod {
  /**
   * @param resp
   */
  private def doUserAgentWorkaround(resp: HttpServletResponse): Unit = {
    if (_userAgent != null && _userAgent.indexOf("WebDAVFS") != -1 && _userAgent.indexOf("Transmit") == -1) {
      LOG.trace("DoPut.execute() : do workaround for user agent '" + _userAgent + "'")
      resp.setStatus(WebdavStatus.SC_CREATED)
    }
    else if (_userAgent != null && _userAgent.indexOf("Transmit") != -1) {
      LOG.trace("DoPut.execute() : do workaround for user agent '" + _userAgent + "'")
      resp.setStatus(WebdavStatus.SC_NO_CONTENT)
    }
    else {
      resp.setStatus(WebdavStatus.SC_CREATED)
    }
  }

  def this(store: Nothing, resLocks: Nothing, readOnly: Boolean, lazyFolderCreationOnPut: Boolean) {
    this ()
    _store = store
    _resourceLocks = resLocks
    _readOnly = readOnly
    _lazyFolderCreationOnPut = lazyFolderCreationOnPut
  }

  private var _resourceLocks: Nothing = null

  def execute(transaction: Nothing, req: HttpServletRequest, resp: HttpServletResponse): Unit = {
    LOG.trace("-- " + this.getClass.getName)
    if (!_readOnly) {
      var path: Nothing = getRelativePath(req)
      var parentPath: Nothing = getParentPath(path)
      _userAgent = req.getHeader("User-Agent")
      var errorList: Hashtable[Nothing, Nothing] = new Hashtable[Nothing, Nothing]
      if (!checkLocks(transaction, req, resp, _resourceLocks, parentPath)) {
        errorList.put(parentPath, WebdavStatus.SC_LOCKED)
        sendReport(req, resp, errorList)
        return
      }
      if (!checkLocks(transaction, req, resp, _resourceLocks, path)) {
        errorList.put(path, WebdavStatus.SC_LOCKED)
        sendReport(req, resp, errorList)
        return
      }
      var tempLockOwner: Nothing = "doPut" + System.currentTimeMillis + req.toString
      if (_resourceLocks.lock(transaction, path, tempLockOwner, false, 0, TEMP_TIMEOUT, TEMPORARY)) {
        var parentSo: Nothing = null
        var so: Nothing = null
        try {
          parentSo = _store.getStoredObject(transaction, parentPath)
          if (parentPath != null && parentSo != null && parentSo.isResource) {
            resp.sendError(WebdavStatus.SC_FORBIDDEN)
            return
          }
          else if (parentPath != null && parentSo == null && _lazyFolderCreationOnPut) {
            _store.createFolder(transaction, parentPath)
          }
          else if (parentPath != null && parentSo == null && !_lazyFolderCreationOnPut) {
            errorList.put(parentPath, WebdavStatus.SC_NOT_FOUND)
            sendReport(req, resp, errorList)
            return
          }
          so = _store.getStoredObject(transaction, path)
          if (so == null) {
            _store.createResource(transaction, path)
          }
          else {
            if (so.isNullResource) {
              var nullResourceLo: Nothing = _resourceLocks.getLockedObjectByPath(transaction, path)
              if (nullResourceLo == null) {
                resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR)
                return
              }
              var nullResourceLockToken: Nothing = nullResourceLo.getID
              var lockTokens: Array[Nothing] = getLockIdFromIfHeader(req)
              var lockToken: Nothing = null
              if (lockTokens != null) {
                lockToken = lockTokens(0)
              }
              else {
                resp.sendError(WebdavStatus.SC_BAD_REQUEST)
                return
              }
              if (lockToken.equals(nullResourceLockToken)) {
                so.setNullResource(false)
                so.setFolder(false)
                var nullResourceLockOwners: Array[Nothing] = nullResourceLo.getOwner
                var owner: Nothing = null
                if (nullResourceLockOwners != null) owner = nullResourceLockOwners(0)
                if (!_resourceLocks.unlock(transaction, lockToken, owner)) {
                  resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR)
                }
              }
              else {
                errorList.put(path, WebdavStatus.SC_LOCKED)
                sendReport(req, resp, errorList)
              }
            }
          }
          doUserAgentWorkaround(resp)
          var resourceLength: Long = _store.setResourceContent(transaction, path, req.getInputStream, null, null)
          so = _store.getStoredObject(transaction, path)
          if (resourceLength != -1) so.setResourceLength(resourceLength)
        }
        catch {
          case e: Nothing => {
            resp.sendError(WebdavStatus.SC_FORBIDDEN)
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

  private var _readOnly: Boolean = false
  private var _userAgent: Nothing = null
  private var _store: Nothing = null
  private var _lazyFolderCreationOnPut: Boolean = false
}

