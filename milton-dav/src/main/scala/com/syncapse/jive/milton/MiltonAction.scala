package com.syncapse.jive.milton

import com.jivesoftware.community.action.JiveActionSupport
import com.bradmcevoy.http.{ServletResponse, ServletRequest}
import com.jivesoftware.community.action.util.AlwaysAllowAnonymous
import javax.servlet.http.HttpServletResponse

@AlwaysAllowAnonymous
class MiltonAction extends JiveActionSupport with WebdavManagerComponent {

  override def execute = {
    webdavManger.process(new ServletRequest(getRequest), new ServletResponse(getResponse))
    "none"
  }

}