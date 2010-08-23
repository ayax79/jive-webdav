package com.syncapse.jive.webdav

import net.sf.webdav.WebdavServlet
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}

object WebdavProcessor {
  protected lazy val webdav = {
    val wd = new WebdavServlet()
    wd.init() // initialize shit here
    wd
  }

  def process(req: HttpServletRequest, resp: HttpServletResponse) = {
    webdav.service(req, resp)
  }


}