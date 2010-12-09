package com.syncapse.jive.milton

import com.jivesoftware.community.action.JiveActionSupport
import com.bradmcevoy.http.{ServletResponse, ServletRequest}
import com.jivesoftware.community.action.util.AlwaysAllowAnonymous

@AlwaysAllowAnonymous
class MiltonAction extends JiveActionSupport with WebdavManagerSupport {

  override def execute = {
    webdavManger.process(new ServletRequest(getRequest), new ServletResponse(getResponse))
    "none"
  }

}