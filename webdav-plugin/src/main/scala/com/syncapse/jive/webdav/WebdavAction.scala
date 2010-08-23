package com.syncapse.jive.webdav

import com.jivesoftware.community.action.JiveActionSupport

class WebdavAction extends JiveActionSupport {
  override def execute = {
    WebdavProcessor.process(getRequest, getResponse)
    "success"
  }
}