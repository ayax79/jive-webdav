package com.syncapse.jive.milton

import org.springframework.beans.factory.annotation.Required
import com.bradmcevoy.http.webdav.DefaultWebDavResponseHandler
import com.bradmcevoy.http._
import com.ettrema.console._
import collection.JavaConversions
import com.ettrema.http.fs.{SimpleSecurityManager, FsMemoryLockManager, FileSystemResourceFactory}

trait WebdavManager {
  def process(req: Request, resp: Response)
}

trait WebdavManagerComponent {
  protected[milton] var webdavManger: WebdavManager = null

  @Required
  def setWebdavManager(wd: WebdavManager) = {
    webdavManger = wd
  }
}

class WebdavManagerImpl(val securityManager: SecurityManager) extends WebdavManager {

  protected val lockManager = new FsMemoryLockManager
  protected val authService = new AuthenticationService
  authService.setDisableBasic(true)
  authService.setDisableDigest(false)
  protected val webDavHandler = new DefaultWebDavResponseHandler(authService)
  protected val compressingResponseHandler = new CompressingResponseHandler(webDavHandler)

  // initialized in init do to dependencies on injected resources
  protected var fileResourceFactory = new FileSystemResourceFactory
  fileResourceFactory.setSecurityManager(securityManager)
  fileResourceFactory.setLockManager(lockManager)
  fileResourceFactory.setMaxAgeSeconds(3600)
  fileResourceFactory.setContextPath("jive/webdav")

  protected var consoleResourceFactory = new ConsoleResourceFactory(fileResourceFactory, "/jive/webdav/console", "/jive/webdav",
    JavaConversions.asJavaList(List(new LsFactory, new CdFactory, new RmFactory, new HelpFactory, new CpFactory, new MkFactory, new MkdirFactory)),
    "jive/webdav");

  protected var mgr = new HttpManager(fileResourceFactory, compressingResponseHandler, authService)


  override def process(req: Request, resp: Response) = mgr.process(req, resp)
}