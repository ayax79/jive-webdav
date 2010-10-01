package com.syncapse.jive.webdav

import javax.servlet._
import http.{HttpServletResponse, HttpServletRequest}
import reflect.BeanProperty
import com.syncapse.jive.Loggable
import org.springframework.context.{ApplicationContext, ApplicationContextAware}
import org.springframework.web.context.WebApplicationContext
import com.jivesoftware.community.JiveHome
import java.io.File
import net.sf.webdav.{LocalFileSystemStore, IWebdavStore, WebDavServletBean}

/**
 * An acegi filter that wraps the WebdavServlet
 */
class WebdavFilter extends Filter with Loggable with ApplicationContextAware with ContextProvider with JiveAuthenticationProvidable {
  @BeanProperty
  var applicationContext: ApplicationContext = null

  protected var webdav: WebDavServletBean = null

  def init = {
    logger.info("WebdavFilter init called")

    val tmpRoot = new File(JiveHome.getCache, "webdav")
    if (!tmpRoot.exists) tmpRoot.mkdirs()

    val tmpStore: IWebdavStore = new LocalFileSystemStore(tmpRoot)

    val store = new JiveWebdavStore(this, tmpStore)
    webdav = new WebDavServletBean {
      // The webdav servlet needs access to the servlet context
      override def getServletContext = {
        applicationContext.asInstanceOf[WebApplicationContext].getServletContext
      }
    }
    webdav.init(store, null, null, 1, false)
  }

  def destroy = {
    logger.info("WebdavFilter destroy called")
  }

  def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) = {
    logger.info("WebdavFilter doFilter called path: " + request.asInstanceOf[HttpServletRequest].getRequestURI)

    jiveAuthentication match {
      case Some(a) => a.isAnonymous match {
        case true => forceAuth(response) 
        case false => webdav.service(request, response)
      }
      case None => forceAuth(response)
    }
  }

  def init(p1: FilterConfig) = {
    // isn't called with spring 
  }

  protected def forceAuth(servletResponse: ServletResponse) = {
    val httpResponse = servletResponse.asInstanceOf[HttpServletResponse]
    httpResponse.addHeader("WWW-Authenticate", "Basic realm=\"Jive SBS\"");
    httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication Required");
  }
}