package com.syncapse.jive.webdav

import com.jivesoftware.community.JiveContext
import com.jivesoftware.community.lifecycle.JiveApplication

trait ContextProvider {
  def jiveContext: JiveContext = JiveApplication.getEffectiveContext
}