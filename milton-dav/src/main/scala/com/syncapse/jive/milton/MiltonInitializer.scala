package com.syncapse.jive.milton

import com.jivesoftware.community.lifecycle.{ApplicationState, ApplicationStateChangeEvent}
import com.syncapse.jive.servlet.{AppServerUtils, ServletContextComponent}
import com.syncapse.jive.event.ApplicationStateChangeListener

object MiltonInitializer {
  protected[milton] var webdavManager: WebdavManager = null
}

class MiltonInitializer extends ApplicationStateChangeListener with ServletContextComponent with WebdavManagerComponent {

  def handle(e: ApplicationStateChangeEvent) = e.getNewState match {
    case ApplicationState.RUNNING => initialize
    case ApplicationState.SHUTDOWN => shutdown
  }

  protected def initialize = {
    MiltonInitializer.webdavManager = webdavManger
    AppServerUtils.registerFilter(servletContext, "webdav", classOf[MiltonFilter].getName, "/webdav", getClass.getClassLoader)
  }

  protected def shutdown = {
    MiltonInitializer.webdavManager = null
  }


}