package com.syncapse.jive.webdav

import java.security.Principal
import java.lang.String
import com.syncapse.jive.Loggable
import net.sf.webdav.{StoredObject, ITransaction, IWebdavStore}
import scala.collection.JavaConversions
import com.jivesoftware.community._
import java.net.URLEncoder
import java.io.{ByteArrayInputStream, InputStream}
import net.sf.webdav.exceptions.WebdavException

class JiveWebdavStore(contextProvider: ContextProvider) extends IWebdavStore with Loggable {
  protected def documentManager = contextProvider.jiveContext.getDocumentManager

  protected def communityManager = contextProvider.jiveContext.getCommunityManager

  def getStoredObject(transaction: ITransaction, uri: String) = {
    val so = JiveWebdavUtils.matchUrl(uri) {
      case CommunityUri(s) => s match {
        case "" => Some(JiveWebdavUtils.RootStoredObject)
        case _ => findObjectFromUriTokens(tokens(s), rootCommunity) match {
          case None => None
          case Some(x) => Some(JiveWebdavUtils.buildStoredObject(x))
        }
      }
      case SpacesUri(s) => s match {
        case "" => Some(JiveWebdavUtils.buildStoredObject(rootCommunity))
        case _ => None
      }
      case RootUri => Some(JiveWebdavUtils.RootStoredObject)
      case _ => None
    } match {
      case Some(x) => x
      case None => null
    }
    logger.info("JiveWebStore getStoredObject: " + uri + " storedObject: " + printSo(so))
    so
  }

  def removeObject(transaction: ITransaction, uri: String) = {
    logger.info("JiveWebStore removeObject: " + uri)
    throw new WebdavException("unsupported operation")
  }

  def getResourceLength(transaction: ITransaction, path: String) = {
    logger.info("JiveWebStore getResourceLength: " + path)
    JiveWebdavUtils.matchUrl(path) {
      case CommunityUri(rest) => findObjectFromUriTokens(tokens(rest), rootCommunity) match {
        case Some(x) => x match {
          case DocumentCase(d) => Some(JiveWebdavUtils.buildStoredObject(x).getResourceLength)
          case _ => None
        }
        case None => None
      }
    } match {
      case Some(s) => s
      case None => 0L
    }
  }

  def getChildrenNames(transaction: ITransaction, folderUri: String) = {
    def communityNames(community: Community) = {
      val communities: Iterable[Community] = JavaConversions.asIterable(communityManager.getCommunities(community))
      communities.map(c => c.getName).toArray[String]
    }

    def documentNames(jco: JiveContainer) = {
      val docs = loadDocuments(jco)
      docs.map(d => documentTitle(d)).toArray[String]
    }

    val children = JiveWebdavUtils.matchUrl(folderUri) {
      case RootUri => Some(Array("communities", "spaces"))
      case CommunityUri(rest) => rest match {
        case "" => Some(communityNames(rootCommunity.jiveObject) ++ documentNames(rootCommunity.jiveObject))
        case _ => findObjectFromUriTokens(tokens(rest), rootCommunity) match {
          case Some(x) => x match {
            case CommunityCase(c) => Some(communityNames(c) ++ documentNames(c))
            case _ => None
          }
          case _ => None
        }
      }
      case _ => None
    } match {
      case Some(x) => x
      case None => null
    }

    logger.info("JiveWebStore getChildrenNames: " + folderUri + " children: " + printList(children))
    children
  }


  def setResourceContent(transaction: ITransaction, resourceUri: String, content: InputStream, contentType: String, characterEncoding: String) = {
    logger.info("JiveWebStore setResourceContent: " + resourceUri)
    throw new WebdavException("unsupported operation")
  }

  def getResourceContent(transaction: ITransaction, resourceUri: String) = JiveWebdavUtils.matchUrl(resourceUri) {
    case CommunityUri(rest) => rest match {
      case "" => None
      case _ => findObjectFromUriTokens(tokens(rest), rootCommunity) match {
        case Some(x) =>
          x match {
            case DocumentCase(d) =>
              d.isTextBody match {
                case true => Option(new ByteArrayInputStream(d.getPlainBody.getBytes("utf-8")))
                case false => Option(d.getBinaryBody.getData)
              }
            case _ => null
          }
        case _ => null
      }
    }
    case _ => None
  } match {
    case Some(s) => s
    case None => null
  }


  def createResource(transaction: ITransaction, resourceUri: String) = {
    logger.info("JiveWebStore createResource: " + resourceUri)
    throw new WebdavException("unsupported operation")
  }

  def createFolder(transaction: ITransaction, folderUri: String) = {
    logger.info("JiveWebStore createFolder: " + folderUri)
    JiveWebdavUtils.matchUrl(folderUri) {
      case CommunityUri(rest) => rest match {
        case "" => None
        case _ =>
          tokens(rest).reverse match {
            case Nil => None
            case head :: tail =>
              findObjectFromUriTokens(tail.reverse, rootCommunity) match {
                case Some(x) => x match {
                  case CommunityCase(c) =>
                    try {
                      Some(communityManager.createCommunity(c, head, head, head))
                    }
                    catch {
                      case e: Exception =>
                        logger.warn(e.getMessage, e)
                        throw new WebdavException(e)
                    }
                  case _ => None
                }
                case None => None
              }
          }
      }
    } match {
      case Some(s) => s
      case None => new WebdavException("Cannot create a folder at: " + folderUri)
    }
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

  def rootCommunity = CommunityCase(communityManager.getRootCommunity)

  def matchingCommunity(c: Community, name: String): Option[CommunityCase] = {
    logger.info("matchingCommunity c: " + c.getName + " name: " + name)
    // todo, caching or something more efficient here
    val communities: Iterable[Community] = JavaConversions.asIterable(communityManager.getCommunities(c))
    communities.find(c => c.getName == name) match {
      case Some(x) => Some(CommunityCase(x))
      case None => None
    }
  }

  def findObjectFromUriTokens(tokens: List[String], j: JiveCaseClass): Option[JiveCaseClass] = j match {
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
                case _ => findObjectFromUriTokens(tail, j)
              }
            case _ =>
              matchingCommunity(c, head) match {
                case None =>
                  // see if it a document instead
                  documentFromCommunity(head, c) match {
                    case None => None
                    case Some(d) => findObjectFromUriTokens(tail, d)
                  }
                case Some(c) =>
                  tail match {
                    case Nil => Option(c) // return the community we are out of tokens.
                    case _ => findObjectFromUriTokens(tail, c)
                  }
              }
          }

      }
  }

  def documentFromCommunity(s: String, c: Community): Option[DocumentCase] = {
    logger.info("documentFromCommunity s: " + s + " c: " + c)
    val docs: Iterable[Document] = loadDocuments(c)
    docs.find(d => documentTitle(d) == s) match {
      case None => None
      case Some(d) => Some(DocumentCase(d))
    }
  }

  def printSo(so: StoredObject) = if (so != null) {
    "folder: " + so.isFolder + ",creationDate" + so.getCreationDate + ",lastModified: " + so.getLastModified
  } else {
    " NULL "
  }


  def printList(list: Array[String]): String = list match {
    case null => null
    case _ => printList(list.toList)
  }

  def printList(list: List[String]): String = list match {
    case Nil => ""
    case _ =>
      "[" + list.reduceLeft(_ + ", " + _) + "]"
  }

  def tokens(uri: String) = {
    val list: List[String] = uri.split("/").toList
    list.filter {
      case "" => false;
      case _ => true
    }
  }

  def documentTitle(d: Document): String = {
    d.isTextBody match {
      case true => d.getSubject
      case false => d.getBinaryBody.getName
    }
  }

  def loadDocuments(c: JiveContainer): List[Document] = {
    val filter = DocumentResultFilter.createDefaultFilter
    filter.setRecursive(false)
    val list: List[Document] = JavaConversions.asIterable(documentManager.getDocuments(c, filter)).toList
    // right now we only want binary documentts and it appears that for some reason there is a bug
    // in jive where recursive docs are always returned, double check.
    list.filter {
      d => !d.isTextBody && c.getID == d.getContainerID
    }
  }

}