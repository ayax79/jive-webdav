package com.syncapse.jive.webdav

import org.specs.mock.Mockito
import org.specs.Specification
import org.specs.runner.JUnit4
import scala.collection.JavaConversions.asList
import com.jivesoftware.community._
import impl.{EmptyJiveIterator, ListJiveIterator}
import org.mockito.{Matchers => M}

class JiveWebdavStoreTest extends JUnit4(JiveWebdavStoreSpec)
object JiveWebdavStoreSpec extends Specification with Mockito with ContextProvider {
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
  documentManager.getDocuments(M.eq(root), any[DocumentResultFilter]) returns emptyJiveIterator

  val two = mock[Community]
  two.getName returns "two"
  communityManager.getCommunities(one) returns newJiveIterator(two)
  communityManager.getCommunities(two) returns emptyJiveIterator
  documentManager.getDocuments(M.eq(two), any[DocumentResultFilter]) returns emptyJiveIterator

  val doc1 = mock[Document]
  doc1.getSubject returns "doc1"
  documentManager.getDocuments(M.eq(one), any[DocumentResultFilter]) returns newJiveIterator(doc1)

  val binaryBody = mock[BinaryBody]
  doc1.getBinaryBody returns binaryBody
  binaryBody.getName returns "doc1"


  override def jiveContext = mockContext

  val store = new JiveWebdavStore(this, null)

  "The webdav store children " should {

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

    "be empty" in {
      val list = store.getChildrenNames(null, "/communities/one/two")
      list.length must be(0)
    }

    "still be empty" in {
      val list = store.getChildrenNames(null, "/communities/one/two/")
      list.length must be(0)
    }

  }
  
  "findCommunityUrl" should {

    "return one" in {
      store.findObjectFromUriTokens(List("one"), root) match {
        case Some(c: Community) => c must be(one)
        case _ => fail("Did not contain a Some(CommunityCase)")
      }
    }

    "return two" in {
      store.findObjectFromUriTokens(List("one", "two"), root) match {
        case Some(c: Community) => c must be(two)
        case _ => fail("Did not contain a Some(CommunityCase)")
      }
    }

    "return doc1" in {
      store.findObjectFromUriTokens(List("one", "doc1"), root) match {
        case Some(d: Document) => d must be(doc1)
        case _ => fail("Did not contain a Some(DocumentCase)")
      }
    }
  }

  "createFolder" should {

    "add a new community" in {
      store.createFolder(null, "/communities/one/bah")
      there was one(communityManager).createCommunity(one, "bah", "bah", "bah")
    }

  }

  "creatNewItem" should {
    "handle function call" in {
      store.createNewItem(List("one", "two", "three")) {
        case (name: String, jc: JiveObject) => jc match {
          case c: Community =>
            c must be(two)
            name must be("three")
            None
          case _ => fail("did not have a community case")
        }

        case _ => fail("should end up here")
      }
    }
  }

  "some case class check stuff" should {

    "work with communities" in {

      val jo: JiveObject = one
      jo match {
        case c: Community => c must be(one)
        case d: Document => fail("should not be a document")
        case _ => fail("didn't match")
      }
    }


  }



  def newJiveIterator[A <: JiveObject](args: A*) = new ListJiveIterator[A](asList(args.toList))

  def emptyJiveIterator[A <: JiveObject] = EmptyJiveIterator.getInstance[A]

}