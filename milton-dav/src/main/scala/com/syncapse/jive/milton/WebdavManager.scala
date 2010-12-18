package com.syncapse.jive.milton

import com.bradmcevoy.http.webdav.DefaultWebDavResponseHandler
import com.bradmcevoy.http._
import com.ettrema.console._
import collection.JavaConversions._
import com.ettrema.http.fs.FsMemoryLockManager
import com.jivesoftware.community.JiveContext
import org.springframework.context.{ApplicationContext, ApplicationContextAware}

trait WebdavManager {
  def process(req: Request, resp: Response)
}

class WebdavManagerImpl(sm: SecurityManager) extends WebdavManager with ApplicationContextAware {

  protected var jc: JiveContext = _

  def setApplicationContext(p1: ApplicationContext) = {
    jc = p1.asInstanceOf[JiveContext]
  }

  protected var mgr: HttpManager = _

  protected def init = {
    val lockManager = new FsMemoryLockManager
    val authService = new AuthenticationService
    authService.setDisableBasic(false)
    authService.setDisableDigest(true)

    val webDavHandler = new DefaultWebDavResponseHandler(authService)
    val compressingResponseHandler = new CompressingResponseHandler(webDavHandler)
    val resourceFactory: JiveResourceFactory = new JiveResourceFactory(jc, sm)
    resourceFactory.contextPath = "jive/webdav"

    val factories = asJavaList(List(new LsFactory, new CdFactory, new RmFactory, new HelpFactory,
      new CpFactory, new MkFactory, new MkdirFactory))

    val consoleResourceFactory = new ConsoleResourceFactory(resourceFactory, "/jive/webdav/console", "/jive/webdav",
      factories, "jive/webdav");

    mgr = new HttpManager(resourceFactory, compressingResponseHandler, authService)
  }


  override def process(req: Request, resp: Response) = mgr.process(req, resp)
}