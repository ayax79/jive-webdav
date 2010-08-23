package com.syncapse.jive.webdav

import javax.servlet._
import com.jivesoftware.community.{DocumentManager, CommunityManager}
import reflect.BeanProperty
import org.springframework.beans.factory.annotation.Required
import net.sf.webdav.WebdavServlet

/**
 * An acegi filter that wraps the WebdavServlet
 */
class WebdavFilter extends Filter {

  @BeanProperty
  @Required
  var communityManager: CommunityManager = null

  @BeanProperty
  @Required
  var documentManager: DocumentManager = null

  protected var webdav: WebdavServlet = null

  var init = {
    val store = new JiveWebdavStore(communityManager, documentManager)
    webdav = new WebdavServlet
    webdav.init(webdav, )
  }


  def destroy = {}

  def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) = {
    webdav.service(request, response)
  }

  def init(p1: FilterConfig) = {
    // isn't called with spring 
  }
}