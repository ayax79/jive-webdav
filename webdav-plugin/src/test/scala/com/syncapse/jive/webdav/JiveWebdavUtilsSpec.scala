package com.syncapse.jive.webdav

import org.specs.Specification
import org.specs.runner.JUnit4
import com.syncapse.jive.webdav.JiveWebdavUtils._
import com.jivesoftware.community.{Document, Community}
import org.specs.mock.Mockito

class jiveWebdavUtilsTest extends JUnit4(JiveWebdavUtilsSpec)
object JiveWebdavUtilsSpec extends Specification with org.specs.mock.Mockito {

  "buildStoredObject" should {

    "build a StoredObject for a Community in " in {
      val c = mock[Community]
      val result = buildStoredObject(CommunityCase(c))
      result must notBeNull
      result.isFolder must be(true)
      result.isResource must be (false)
    }

    "build a StoredObject for a Document in" in {
      val d = mock[Document]
      d.isTextBody returns true
      d.getPlainBody returns "This is a plain body"
      val result = buildStoredObject(DocumentCase(d))
      result must notBeNull
      result.isFolder must be(false)
      result.isResource must be(true)
    }
  }

}