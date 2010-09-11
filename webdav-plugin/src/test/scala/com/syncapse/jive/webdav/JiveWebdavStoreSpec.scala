package com.syncapse.jive.webdav

import org.specs.mock.Mockito
import org.specs.Specification
import org.specs.runner.JUnit4
import scala.collection.JavaConversions.asList
import com.jivesoftware.community._
import impl.{EmptyJiveIterator, ListJiveIterator}

class JiveWebdavStoreTest extends JUnit4(JiveWebdavStoreSpec)
object JiveWebdavStoreSpec extends Specification with Mockito {
  val mockContext = mock[JiveContext]

  val communityManager = mock[CommunityManager]
  mockContext.getCommunityManager returns communityManager

  val documentManager = mock[DocumentManager]
  mockContext.getDocumentManager returns documentManager

  val root = mock[Community]
  communityManager.getRootCommunity returns root

  val one = mock[Community]
  one.getName returns "one"
  communityManager.getCommunities(root) returns newJiveIterator(one)
  documentManager.getDocuments(root) returns emptyJiveIterator

  val two = mock[Community]
  two.getName returns "two"
  communityManager.getCommunities(one) returns newJiveIterator(two)

  val doc1 = mock[Document]
  doc1.getSubject returns "doc1"
  documentManager.getDocuments(one) returns newJiveIterator(doc1)

  val store = new JiveWebdavStore(mockContext)

  "The webdav store children should " should {

    "be communities and spaces" in {
      val list = store.getChildrenNames(null, "/").toList
      list must haveSize(2)
      list must contain("communities")
      list must contain("spaces")
    }

    "be one" in {
      val list = store.getChildrenNames(null, "/communities").toList
      list must haveSize(1)
      list must contain("one")
    }

    "be two and doc1" in {
      val list = store.getChildrenNames(null, "/communities/one").toList
      list must haveSize(2)
      list must contain("two")
      list must contain("doc1")
    }

  }


  "findCommunityUrl" should {

    "return one" in {
      store.findCommunityUri(List("one"), CommunityCase(root)) match {
        case Some(c: CommunityCase) => c.jiveObject must be(one)
        case _ => fail("Did not contain a Some(CommunityCase)")
      }
    }

    "return two" in {
      store.findCommunityUri(List("one", "two"), CommunityCase(root)) match {
        case Some(c: CommunityCase) => c.jiveObject must be(two)
        case _ => fail("Did not contain a Some(CommunityCase)")
      }
    }

    "return doc1" in {
      store.findCommunityUri(List("one", "doc1"), CommunityCase(root)) match {
        case Some(d: DocumentCase) => d.jiveObject must be(doc1)
        case _ => fail("Did not contain a Some(DocumentCase)")
      }
    }
  }

  def newJiveIterator[A <: JiveObject](args: A*) = new ListJiveIterator[A](asList(args.toList))

  def emptyJiveIterator[A <: JiveObject] = EmptyJiveIterator.getInstance[A]

}