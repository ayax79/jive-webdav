package com.syncapse.jive.webdav

import com.jivesoftware.base.plugin.Plugin
import com.syncapse.jive.Loggable

class ScalaPlugin extends Plugin[ScalaPlugin] with Loggable {
  def destroy = {
    logger.info("ScalaPlugin is being destroyed")
  }

  def init = {
    logger.info("ScalaPlugin is being initialized")
  }
}