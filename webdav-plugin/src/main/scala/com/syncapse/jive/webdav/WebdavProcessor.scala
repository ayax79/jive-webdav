package com.syncapse.jive.webdav

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.Principal
import java.util.Enumeration
import java.util.HashMap
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import net.sf.webdav.exceptions.UnauthenticatedException
import net.sf.webdav.exceptions.WebdavException
import net.sf.webdav.fromcatalina.MD5Encoder
import net.sf.webdav.locking.ResourceLocks
import net.sf.webdav.methods.DoCopy
import net.sf.webdav.methods.DoDelete
import net.sf.webdav.methods.DoGet
import net.sf.webdav.methods.DoHead
import net.sf.webdav.methods.DoLock
import net.sf.webdav.methods.DoMkcol
import net.sf.webdav.methods.DoMove
import net.sf.webdav.methods.DoNotImplemented
import net.sf.webdav.methods.DoOptions
import net.sf.webdav.methods.DoPropfind
import net.sf.webdav.methods.DoProppatch
import net.sf.webdav.methods.DoPut
import net.sf.webdav.methods.DoUnlock
import org.slf4j.Logger
import java.io.{PrintWriter, StringWriter, IOException}

object WebdavProcessor {
  /**
   * The MD5 helper object for this class.
   */
  protected val MD5_ENCODER: Nothing = new Nothing
  private val READ_ONLY: Boolean = false

  /**
   * MD5 message digest provider.
   */
  protected var MD5_HELPER: MessageDigest = null
  private var LOG: Logger = org.slf4j.LoggerFactory.getLogger(WebdavProccesor.getClass)


  def init(store: Nothing, dftIndexFile: Nothing, insteadOf404: Nothing, nocontentLenghHeaders: Int, lazyFolderCreationOnPut: Boolean): Unit = {
    _store = store
    var mimeTyper: Nothing = new Nothing {
      def getMimeType(path: Nothing): Nothing = {
        return getServletContext.getMimeType(path)
      }
    }
    register("GET", new Nothing(store, dftIndexFile, insteadOf404, _resLocks, mimeTyper, nocontentLenghHeaders))
    register("HEAD", new Nothing(store, dftIndexFile, insteadOf404, _resLocks, mimeTyper, nocontentLenghHeaders))
    var doDelete: Nothing = register("DELETE", new Nothing(store, _resLocks, READ_ONLY)).asInstanceOf[Nothing]
    var doCopy: Nothing = register("COPY", new Nothing(store, _resLocks, doDelete, READ_ONLY)).asInstanceOf[Nothing]
    register("LOCK", new Nothing(store, _resLocks, READ_ONLY))
    register("UNLOCK", new Nothing(store, _resLocks, READ_ONLY))
    register("MOVE", new Nothing(_resLocks, doDelete, doCopy, READ_ONLY))
    register("MKCOL", new Nothing(store, _resLocks, READ_ONLY))
    register("OPTIONS", new Nothing(store, _resLocks))
    register("PUT", new Nothing(store, _resLocks, READ_ONLY, lazyFolderCreationOnPut))
    register("PROPFIND", new Nothing(store, _resLocks, mimeTyper))
    register("PROPPATCH", new Nothing(store, _resLocks, READ_ONLY))
    register("*NO*IMPL*", new Nothing(READ_ONLY))
  }

  private var _methodMap: HashMap[Nothing, Nothing] = new HashMap[Nothing, Nothing]

  def this() {
    this ()
    _resLocks = new Nothing
    try {
      MD5_HELPER = MessageDigest.getInstance("MD5")
    }
    catch {
      case e: NoSuchAlgorithmException => {
        throw new Nothing
      }
    }
  }

  private def register(methodName: Nothing, method: Nothing): Nothing = {
    _methodMap.put(methodName, method)
    return method
  }

  private var _resLocks: Nothing = null

  private def debugRequest(methodName: Nothing, req: HttpServletRequest): Unit = {
    LOG.trace("-----------")
    LOG.trace("WebdavServlet\n request: methodName = " + methodName)
    LOG.trace("time: " + System.currentTimeMillis)
    LOG.trace("path: " + req.getRequestURI)
    LOG.trace("-----------")
    var e: Enumeration[_] = req.getHeaderNames
    while (e.hasMoreElements) {
      var s: Nothing = e.nextElement.asInstanceOf[Nothing]
      LOG.trace("header: " + s + " " + req.getHeader(s))
    }
    e = req.getAttributeNames
    while (e.hasMoreElements) {
      var s: Nothing = e.nextElement.asInstanceOf[Nothing]
      LOG.trace("attribute: " + s + " " + req.getAttribute(s))
    }
    e = req.getParameterNames
    while (e.hasMoreElements) {
      var s: Nothing = e.nextElement.asInstanceOf[Nothing]
      LOG.trace("parameter: " + s + " " + req.getParameter(s))
    }
  }

  /**
   * Handles the special WebDAV methods.
   */
  def service(req: HttpServletRequest, resp: HttpServletResponse) = {
    var methodName: Nothing = req.getMethod
    var transaction: Nothing = null
    var needRollback: Boolean = false
    if (LOG.isTraceEnabled) debugRequest(methodName, req)
    try {
      var userPrincipal: Principal = req.getUserPrincipal
      transaction = _store.begin(userPrincipal)
      needRollback = true
      _store.checkAuthentication(transaction)
      resp.setStatus(WebdavStatus.SC_OK)
      try {
        var methodExecutor: Nothing = _methodMap.get(methodName).asInstanceOf[Nothing]
        if (methodExecutor == null) {
          methodExecutor = _methodMap.get("*NO*IMPL*").asInstanceOf[Nothing]
        }
        methodExecutor.execute(transaction, req, resp)
        _store.commit(transaction)
        needRollback = false
      }
      catch {
        case e: IOException => {
          var sw: StringWriter = new StringWriter
          var pw: PrintWriter = new PrintWriter(sw)
          e.printStackTrace(pw)
          LOG.error("IOException: " + sw.toString)
          resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR)
          _store.rollback(transaction)
          throw new ServletException(e)
        }
      }
    }
    catch {
      //todo - cases doesn't make sense
      case e: Nothing => {
        resp.sendError(WebdavStatus.SC_FORBIDDEN)
      }
      case e: Nothing => {
        var sw: StringWriter = new StringWriter
        var pw: PrintWriter = new PrintWriter(sw)
        e.printStackTrace(pw)
        LOG.error("WebdavException: " + sw.toString)
        throw new ServletException(e)
      }
      case e: Nothing => {
        var sw: StringWriter = new StringWriter
        var pw: PrintWriter = new PrintWriter(sw)
        e.printStackTrace(pw)
        LOG.error("Exception: " + sw.toString)
      }
    }
    finally {
      if (needRollback) _store.rollback(transaction)
    }
  }

  private var _store: Nothing = null
}

