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
import javax.ws.rs.core.SecurityContext
import org.springframework.security.context.SecurityContextHolder

class JiveWebdavStore(jiveContext: JiveContext) extends IWebdavStore with Loggable {
  lazy val CommunitiesRE = """\/communities\/([^\s]*)$""".r
  lazy val SpacesRE = """\/spaces\/([^\s])*$""".r
  lazy val documentManager = jiveContext.getDocumentManager
  lazy val communityManager = jiveContext.getCommunityManager

  def getStoredObject(transaction: ITransaction, uri: String) = {
    val so = uri match {
      case "/" => RootStoredObject.asInstanceOf[StoredObject]
      case "/communities" => JiveWebdavUtils.buildStoredObject(rootCommunity)
      case "/spaces" => RootStoredObject.asInstanceOf[StoredObject]
      case CommunitiesRE(rest) =>
        findCommunityUri(tokens(rest), rootCommunity) match {
          case None => RootStoredObject.asInstanceOf[StoredObject]
          case Some(x) => JiveWebdavUtils.buildStoredObject(x)
        }
      case _ => RootStoredObject.asInstanceOf[StoredObject] // just put something so we don't get an NPE
    }
    logger.info("JiveWebStore getStoredObject: " + uri + " storedObject: " + printSo(so))
    so
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
    def communityNames(community: Community) = {
      val communities: Iterable[Community] = JavaConversions.asIterable(communityManager.getCommunities(community))
      communities.map(c => c.getName).toArray[String]
    }

    def documentNames(jco: JiveContainer) = {
      val docs: Iterable[Document] = JavaConversions.asIterable(documentManager.getDocuments(jco))
      docs.map(d => encodedSubject(d)).toArray[String]
    }

    val children = folderUri match {
      case "/" => Array[String]("communities", "spaces")
      case "/communities" =>
        communityNames(rootCommunity.jiveObject) ++ documentNames(rootCommunity.jiveObject)
      case CommunitiesRE(rest) =>
        findCommunityUri(tokens(rest), rootCommunity) match {
          case Some(x) =>
            x match {
              case CommunityCase(c) => communityNames(c) ++ documentNames(c)
              case _ => Array[String]() // either a document or None was returned
            }
          case _ => Array[String]()
        }
      case _ => Array[String]() // todo, sometime i want to test to see if the _ matches above will fall to this if not specified.
    }
    logger.info("JiveWebStore getChildrenNames: " + folderUri + " children: " + printList(children.toList))
    children
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
    logger.info("begin called principal: " + principal)
    new ITransaction {
      def getPrincipal = principal
    }
  }

  protected def rootCommunity = CommunityCase(communityManager.getRootCommunity)

  protected def matchingCommunity(c: Community, name: String): Option[CommunityCase] = {
    logger.info("matchingCommunity c: " + c.getName + " name: " + name)
    // todo, caching or something more efficient here
    val communities: Iterable[Community] = JavaConversions.asIterable(communityManager.getCommunities(c))
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
        case Nil => None // couldn't find a match
        case head :: tail =>
          head match {
            // handle the odd case where there may be no entry
            case "" =>
              tail match {
                case Nil => None
                case _ => findCommunityUri(tail, j)
              }
            case _ =>
              matchingCommunity(c, head) match {
                case None =>
                  documentFromCommunity(head, c) match {
                    case None => None
                    case Some(d) => findCommunityUri(tail, d)
                  }
                case Some(c) => findCommunityUri(tail, c)
              }
          }

      }
  }

  protected def documentFromCommunity(s: String, c: Community): Option[DocumentCase] = {
    logger.info("documentFromCommunity s: " + s + " c: " + c)
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

  protected def printSo(so: StoredObject) = if (so != null) {
    "folder: " + so.isFolder + ",creationDate" + so.getCreationDate + ",lastModified: " + so.getLastModified
  } else {""}

  protected def printList(list: List[String]) = list match {
    case Nil => ""
    case _ => "[" + list.reduceLeft(_ + ", " + _) + "]"
  }

  protected def tokens(uri: String) = uri.split("/").toList

}