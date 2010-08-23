package com.syncapse.jive.webdav.methods

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import net.sf.webdav.IMimeTyper
import net.sf.webdav.ITransaction
import net.sf.webdav.IWebdavStore
import net.sf.webdav.StoredObject
import net.sf.webdav.WebdavStatus
import net.sf.webdav.locking.ResourceLocks
import com.syncapse.jive.webdav.WebdavStatus
import org.slf4j.{LoggerFactory, Logger}


object DoGet {
  private var LOG: Logger = LoggerFactory.getLogger(classOf[DoGet])
}
class DoGet extends DoHead {
  protected def folderBody(transaction: Nothing, path: Nothing, resp: HttpServletResponse, req: HttpServletRequest): Unit = {
    var so: Nothing = _store.getStoredObject(transaction, path)
    if (so == null) {
      resp.sendError(HttpServletResponse.SC_NOT_FOUND, req.getRequestURI)
    }
    else {
      if (so.isNullResource) {
        var methodsAllowed: Nothing = DeterminableMethod.determineMethodsAllowed(so)
        resp.addHeader("Allow", methodsAllowed)
        resp.sendError(WebdavStatus.SC_METHOD_NOT_ALLOWED)
        return
      }
      if (so.isFolder) {
        var out: OutputStream = resp.getOutputStream
        var children: Array[Nothing] = _store.getChildrenNames(transaction, path)
        children = if (children == null) Array[Nothing]() else children
        var childrenTemp: Nothing = new Nothing
        childrenTemp.append("Contents of this Folder:\n")
        for (child <- children) {
          childrenTemp.append(child)
          childrenTemp.append("\n")
        }
        out.write(childrenTemp.toString.getBytes)
      }
    }
  }

  protected def doBody(transaction: Nothing, resp: HttpServletResponse, path: Nothing): Unit = {
    try {
      var so: Nothing = _store.getStoredObject(transaction, path)
      if (so.isNullResource) {
        var methodsAllowed: Nothing = DeterminableMethod.determineMethodsAllowed(so)
        resp.addHeader("Allow", methodsAllowed)
        resp.sendError(WebdavStatus.SC_METHOD_NOT_ALLOWED)
        return
      }
      var out: OutputStream = resp.getOutputStream
      var in: InputStream = _store.getResourceContent(transaction, path)
      try {
        var read: Int = -1
        var copyBuffer: Array[Byte] = new Array[Byte](BUF_SIZE)
        while ((({read = in.read(copyBuffer, 0, copyBuffer.length); read})) != -1) {
          out.write(copyBuffer, 0, read)
        }
      }
      finally {
        try {
          in.close
        }
        catch {
          case e: Nothing => {
            LOG.warn("Closing InputStream causes Exception!\n" + e.toString)
          }
        }
        try {
          out.flush
          out.close
        }
        catch {
          case e: Nothing => {
            LOG.warn("Flushing OutputStream causes Exception!\n" + e.toString)
          }
        }
      }
    }
    catch {
      case e: Nothing => {
        LOG.trace(e.toString)
      }
    }
  }

  def this(store: Nothing, dftIndexFile: Nothing, insteadOf404: Nothing, resourceLocks: Nothing, mimeTyper: Nothing, contentLengthHeader: Int) {
    this ()
    `super`(store, dftIndexFile, insteadOf404, resourceLocks, mimeTyper, contentLengthHeader)

  }
}
