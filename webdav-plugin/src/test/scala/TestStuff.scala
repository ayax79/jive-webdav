package com.syncapse.jive.webdav

import com.syncapse.jive.webdav.JiveWebdavStore
import org.junit.Test

/**
 * Created by IntelliJ IDEA.
 * User: andrew
 * Date: Sep 1, 2010
 * Time: 6:36:04 PM
 * To change this template use File | Settings | File Templates.
 */

class TestStuff {


  @Test
  def testCaseCheck = {

    def caseCheck(uri: String) = uri match {
      case "/" => System.out.println("matched /")
      case "/communities" => System.out.println("matched /communities")
      case _ => System.out.println("matched nothing")
    }

    caseCheck("/")
    caseCheck("/communities")
  }


}