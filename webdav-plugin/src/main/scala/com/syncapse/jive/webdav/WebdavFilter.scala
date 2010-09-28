package com.syncapse.jive.webdav

import javax.servlet._
import http.HttpServletRequest
import reflect.BeanProperty
import net.sf.webdav.WebDavServletBean
import com.syncapse.jive.Loggable
import org.springframework.context.{ApplicationContext, ApplicationContextAware}
import org.springframework.web.context.WebApplicationContext
/**
 * An acegi filter that wraps the WebdavServlet
 */
class WebdavFilter extends Filter with Loggable with ApplicationContextAware with ContextProvider {
  @BeanProperty
  var applicationContext: ApplicationContext = null

  protected var webdav: WebDavServletBean = null

  def init = {
    logger.info("WebdavFilter init called")
    val store = new JiveWebdavStore(this)
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
    webdav.service(request, response)
  }

  def init(p1: FilterConfig) = {
    // isn't called with spring 
  }
}