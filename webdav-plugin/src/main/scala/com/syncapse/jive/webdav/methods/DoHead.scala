package com.syncapse.jive.webdav.methods

import java.io.IOException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import net.sf.webdav.IMimeTyper
import net.sf.webdav.StoredObject
import net.sf.webdav.ITransaction
import net.sf.webdav.WebdavStatus
import net.sf.webdav.IWebdavStore
import net.sf.webdav.exceptions.AccessDeniedException
import net.sf.webdav.exceptions.LockFailedException
import net.sf.webdav.exceptions.ObjectAlreadyExistsException
import net.sf.webdav.exceptions.WebdavException
import net.sf.webdav.locking.ResourceLocks
import com.syncapse.jive.webdav.WebdavStatus
import org.slf4j.{LoggerFactory, Logger}


object DoHead {
  private var LOG: Logger = LoggerFactory.getLogger(classOf[DoHead])
}
class DoHead extends AbstractMethod(_store: Nothing, _dftIndexFile: Nothing, _insteadOf404: Nothing, _resourceLocks: Nothing, _mimeTyper: Nothing, _contentLengthHeader: Int) {
  def execute(transaction: Nothing, req: HttpServletRequest, resp: HttpServletResponse): Unit = {
    var bUriExists: Boolean = false
    var path: Nothing = getRelativePath(req)
    LOG.trace("-- " + this.getClass.getName)
    var so: Nothing = _store.getStoredObject(transaction, path)
    if (so == null) {
      if (this._insteadOf404 != null && !_insteadOf404.trim.equals("")) {
        path = this._insteadOf404
        so = _store.getStoredObject(transaction, this._insteadOf404)
      }
    }
    else bUriExists = true
    if (so != null) {
      if (so.isFolder) {
        if (_dftIndexFile != null && !_dftIndexFile.trim.equals("")) {
          resp.sendRedirect(resp.encodeRedirectURL(req.getRequestURI + this._dftIndexFile))
          return
        }
      }
      else if (so.isNullResource) {
        var methodsAllowed: Nothing = DeterminableMethod.determineMethodsAllowed(so)
        resp.addHeader("Allow", methodsAllowed)
        resp.sendError(WebdavStatus.SC_METHOD_NOT_ALLOWED)
        return
      }
      var tempLockOwner: Nothing = "doGet" + System.currentTimeMillis + req.toString
      if (_resourceLocks.lock(transaction, path, tempLockOwner, false, 0, TEMP_TIMEOUT, TEMPORARY)) {
        try {
          var eTagMatch: Nothing = req.getHeader("If-None-Match")
          if (eTagMatch != null) {
            if (eTagMatch.equals(getETag(so))) {
              resp.setStatus(WebdavStatus.SC_NOT_MODIFIED)
              return
            }
          }
          if (so.isResource) {
            if (path.endsWith("/") || (path.endsWith("\\"))) {
              resp.sendError(HttpServletResponse.SC_NOT_FOUND, req.getRequestURI)
            }
            else {
              var lastModified: Long = so.getLastModified.getTime
              resp.setDateHeader("last-modified", lastModified)
              var eTag: Nothing = getETag(so)
              resp.addHeader("ETag", eTag)
              var resourceLength: Long = so.getResourceLength
              if (_contentLength == 1) {
                if (resourceLength > 0) {
                  if (resourceLength <= Integer.MAX_VALUE) {
                    resp.setContentLength(resourceLength.asInstanceOf[Int])
                  }
                  else {
                    resp.setHeader("content-length", "" + resourceLength)
                  }
                }
              }
              var mimeType: Nothing = _mimeTyper.getMimeType(path)
              if (mimeType != null) {
                resp.setContentType(mimeType)
              }
              else {
                var lastSlash: Int = path.replace('\\', '/').lastIndexOf('/')
                var lastDot: Int = path.indexOf(".", lastSlash)
                if (lastDot == -1) {
                  resp.setContentType("text/html")
                }
              }
              doBody(transaction, resp, path)
            }
          }
          else {
            folderBody(transaction, path, resp, req)
          }
        }
        catch {
          case e: Nothing => {
            resp.sendError(WebdavStatus.SC_FORBIDDEN)
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
      folderBody(transaction, path, resp, req)
    }
    if (!bUriExists) resp.setStatus(WebdavStatus.SC_NOT_FOUND)
  }

  protected def folderBody(transaction: Nothing, path: Nothing, resp: HttpServletResponse, req: HttpServletRequest): Unit = {
  }

  protected def doBody(transaction: Nothing, resp: HttpServletResponse, path: Nothing): Unit = {
  }

}