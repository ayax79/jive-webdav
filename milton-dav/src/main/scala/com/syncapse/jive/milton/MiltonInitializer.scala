package com.syncapse.jive.milton

import com.jivesoftware.community.lifecycle.{ApplicationState, ApplicationStateChangeEvent}
import com.syncapse.jive.servlet.{AppServerUtils, ServletContextComponent}
import com.syncapse.jive.event.ApplicationStateChangeListener
import org.springframework.aop.SpringProxy
import com.syncapse.jive.Loggable

object MiltonInitializer {
  protected[milton] var webdavManager: WebdavManager = null
}

class MiltonInitializer extends ApplicationStateChangeListener with SpringProxy with ServletContextComponent with WebdavManagerComponent with Loggable {

  def handle(e: ApplicationStateChangeEvent) = e.getNewState match {
    case ApplicationState.RUNNING => initialize
    case ApplicationState.SHUTDOWN => shutdown
    case _ => logger.debug("ignoring event" + e.getNewState)
  }

  protected def initialize = {
    MiltonInitializer.webdavManager = webdavManger
    AppServerUtils.registerFilter(servletContext, "webdav", classOf[MiltonFilter].getName, "/webdav*")
  }

  protected def shutdown = {
    MiltonInitializer.webdavManager = null
  }

}