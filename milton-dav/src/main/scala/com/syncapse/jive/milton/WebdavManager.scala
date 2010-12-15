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

class WebdavManagerImpl extends WebdavManager {

  private val lockManager = new FsMemoryLockManager

  // todo - there are a lot of things hardcoded in here that will need to be cleaned up.

  private val securityManager = new SimpleSecurityManager("jive", JavaConversions.asJavaMap(Map("admin" -> "admin")))
  private val fileResourceFactory = new FileSystemResourceFactory
  fileResourceFactory.setSecurityManager(securityManager)
  fileResourceFactory.setLockManager(lockManager)
  fileResourceFactory.setMaxAgeSeconds(3600)
  fileResourceFactory.setContextPath("jive/webdav")

  private val consoleResourceFactory = new ConsoleResourceFactory(fileResourceFactory, "/jive/webdav/console", "/jive/webdav",
    JavaConversions.asJavaList(List(new LsFactory, new CdFactory, new RmFactory, new HelpFactory, new CpFactory, new MkFactory, new MkdirFactory)),
    "jive/webdav");

  private val authService = new AuthenticationService
  authService.setDisableBasic(false)
  authService.setDisableDigest(true)

  private val webDavHandler = new DefaultWebDavResponseHandler(authService)
  private val compressingResponseHandler = new CompressingResponseHandler(webDavHandler)

  private val mgr = new HttpManager(fileResourceFactory, compressingResponseHandler, authService)

  override def process(req: Request, resp: Response) = mgr.process(req, resp)
}