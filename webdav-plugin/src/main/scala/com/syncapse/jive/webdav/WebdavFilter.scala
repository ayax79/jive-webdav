package com.syncapse.jive.webdav

import javax.servlet._
import com.jivesoftware.community.{DocumentManager, CommunityManager}
import reflect.BeanProperty
import org.springframework.beans.factory.annotation.Required
import net.sf.webdav.{WebDavServletBean, WebdavServlet}
import com.syncapse.jive.Loggable

/**
 * An acegi filter that wraps the WebdavServlet
 */
class WebdavFilter extends Filter with Loggable {

  @BeanProperty
  @Required
  var communityManager: CommunityManager = null

  @BeanProperty
  @Required
  var documentManager: DocumentManager = null

  protected var webdav: WebDavServletBean = null

  def init = {
    logger.info("WebdavFilter init called")
    if (communityManager == null | documentManager == null) {
      throw new IllegalStateException("managers are required!")
    }
    val store = new JiveWebdavStore(communityManager, documentManager)
    webdav = new WebDavServletBean
    webdav.init(store, null, null, 1, false)
  }

  def destroy = {
    logger.info("WebdavFilter destroy called")
  }

  def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) = {
    logger.info("WebdavFilter init called")
    webdav.service(request, response)
  }

  def init(p1: FilterConfig) = {
    // isn't called with spring 
  }
}