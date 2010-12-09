package com.syncapse.jive.milton

import com.ettrema.http.fs.FileSystemResourceFactory
import com.bradmcevoy.http.{Response, Request, HttpManager}
import org.springframework.beans.factory.annotation.Required

trait WebdavManager {
  def process(req: Request, resp: Response)
}

trait WebdavManagerSupport {
  var webdavManger: WebdavManager = null

  @Required
  def setWebdavManager(wd: WebdavManager) = {
    webdavManger = wd
  }
}

class WebdavManagerImpl extends WebdavManager {
  private val mgr = new HttpManager(new FileSystemResourceFactory)

  override def process(req: Request, resp: Response) = mgr.process(req, resp)

}