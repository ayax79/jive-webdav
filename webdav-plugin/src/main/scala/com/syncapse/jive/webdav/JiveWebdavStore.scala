package com.syncapse.jive.webdav

import java.security.Principal
import java.io.InputStream
import java.lang.String
import com.syncapse.jive.Loggable
import net.sf.webdav.{StoredObject, ITransaction, IWebdavStore}
import java.util.Date
import scala.collection.JavaConversions
import com.jivesoftware.community._
import java.net.URLEncoder

class JiveWebdavStore(jiveContext: JiveContext) extends IWebdavStore with Loggable {
  lazy val CommunitiesRE = """\/communities\/([^\s]*)$""".r
  lazy val SpacesRE = """\/spaces\/([^\s])*$""".r
  lazy val documentManager = jiveContext.getDocumentManager
  lazy val communityManager = jiveContext.getCommunityManager

  def getStoredObject(transaction: ITransaction, uri: String) = {
    logger.info("JiveWebStore getStoredObject: " + uri)
    uri match {
      case "/" => RootStoredObject.asInstanceOf[StoredObject]
      case "/communities" => JiveWebdavUtils.buildStoredObject(rootCommunity)
      case "/spaces" => RootStoredObject.asInstanceOf[StoredObject]
      case CommunitiesRE(rest) =>
        val tokens: Array[String] = rest.split("/")
        findCommunityUri(tokens.toList, rootCommunity) match {
          case None => null
          case Some(x) => JiveWebdavUtils.buildStoredObject(x)
        }
      case _ => null
    }
  }

  def removeObject(transaction: ITransaction, uri: String) = {
    logger.info("JiveWebStore removeObject: " + uri)
    null
  }

  def getResourceLength(transaction: ITransaction, path: String) = {
    logger.info("JiveWebStore getResourceLength: " + path)
    0L
  }

  def getChildrenNames(transaction: ITransaction, folderUri: String) = {
    logger.info("JiveWebStore getChildrenNames: " + folderUri)
    def communityNames(community: Community) = {
      val communities: Iterable[Community] = JavaConversions.asIterable(communityManager.getCommunities(community))
      communities.map(c => c.getName).toArray[String]
    }

    def documentNames(jco: JiveContainer) = {
      val docs: Iterable[Document] = JavaConversions.asIterable(documentManager.getDocuments(jco))
      docs.map(d => encodedSubject(d)).toArray[String]
    }

    folderUri match {
      case "/" => Array[String]("communities", "spaces")
      case "/communities" => communityNames(rootCommunity.jiveObject) ++ documentNames(rootCommunity.jiveObject)
      case _ => matchingCommunity(folderUri) match {
        case Some(x) => communityNames(x.jiveObject)
        case None => Array[String]()
      }
    }
  }

  def setResourceContent(transaction: ITransaction, resourceUri: String, content: InputStream, contentType: String, characterEncoding: String) = {
    logger.info("JiveWebStore setResourceContent: " + resourceUri)
    0L
  }

  def getResourceContent(transaction: ITransaction, resourceUri: String) = {
    logger.info("JiveWebStore getResourceContent: " + resourceUri)
    null
  }

  def createResource(transaction: ITransaction, resourceUri: String) = {
    logger.info("JiveWebStore createResource: " + resourceUri)
    null
  }

  def createFolder(transaction: ITransaction, folderUri: String) = {
    logger.info("JiveWebStore createFolder: " + folderUri)
    null
  }

  def rollback(transaction: ITransaction) = {
    logger.info("rollback called")
  }

  def commit(transaction: ITransaction) = {
    logger.info("commit called")
  }

  def checkAuthentication(transaction: ITransaction) = {
    logger.info("checkAuthentication called")
  }

  def begin(principal: Principal) = {
    logger.info("begin called")
    null
  }

  protected def rootCommunity = CommunityCase(communityManager.getRootCommunity)

  protected def matchingCommunity(name: String): Option[CommunityCase] = {
    // todo, caching or something more efficient here
    val communities: Iterable[Community] = JavaConversions.asIterable(communityManager.getRecursiveCommunities(rootCommunity.jiveObject))
    communities.find(c => c.getName == name) match {
      case Some(x) => Some(CommunityCase(x))
      case None => None
    }
  }

  protected def findCommunityUri(tokens: List[String], j: JiveCaseClass): Option[JiveCaseClass] = j match {
  // go until we find a document, even if there are more tokens (we will ignore them)
    case DocumentCase(d) => Some(DocumentCase(d))
    case CommunityCase(c) =>
      tokens match {
      // must just be a community, return the community
        case Nil => Some(j)
        case head :: tail =>
          matchingCommunity(head) match {
            case None =>
              documentFromCommunity(head, c) match {
                case None => None
                case Some(d) => findCommunityUri(tokens, d)
              }
            case Some(c) => findCommunityUri(tokens, c)
          }
      }
  }

  protected def documentFromCommunity(s: String, c: Community): Option[DocumentCase] = {
    val docs: Iterable[Document] = JavaConversions.asIterable(documentManager.getDocuments(c))
    docs.find(d => encodedSubject(d) == s) match {
      case None => None
      case Some(d) => Some(DocumentCase(d))
    }
  }

  protected def encodedSubject(d: Document) = URLEncoder.encode(d.getSubject, "utf-8")

  protected object RootStoredObject extends StoredObject {
    setFolder(true)
    setLastModified(new Date(0))
    setCreationDate(new Date(0))
  }

  

}