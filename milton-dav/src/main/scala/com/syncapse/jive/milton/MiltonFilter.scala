package com.syncapse.jive.milton

import javax.servlet._
import com.bradmcevoy.http.{ServletRequest => WDRequest, ServletResponse => WDResponse}
import http.{HttpServletResponse, HttpServletRequest}

class MiltonFilter(val webdavManager: WebdavManager) extends Filter {

  def doFilter(p1: ServletRequest, p2: ServletResponse, p3: FilterChain) =
    webdavManager.process(new WDRequest(p1.asInstanceOf[HttpServletRequest]), new WDResponse(p2.asInstanceOf[HttpServletResponse]))

  def init(p1: FilterConfig) = {}

  def destroy = {}

}