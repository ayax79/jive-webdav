package com.syncapse.jive.milton

import java.lang.String
import reflect.BeanProperty
import com.syncapse.jive.Loggable
import resource.{DocumentResource, CommunityResource}
import scala.collection.JavaConversions
import com.jivesoftware.community._
import com.syncapse.jive.auth.JiveAuthenticationProvidable
import com.bradmcevoy.http.{SecurityManager, ResourceFactory}

class JiveResourceFactory(jc: JiveContext, sm: SecurityManager) extends ResourceFactory with Loggable with JiveAuthenticationProvidable {

  @BeanProperty var contextPath: String = _

  def getResource(host: String, path: String) = {
    findObjectFromUriTokens(tokens(stripContext(host)), rootCommunity) match {
      case c: Community => new CommunityResource(c, jc, sm)
      case d: Document => new DocumentResource(d, jc.getDocumentManager, sm)
      case _ => throw new IllegalArgumentException("resource already exists")
    }
  }

  protected def stripContext(url: String): String = {
    var url2 = url
    if (this.contextPath != null && contextPath.length() > 0) {
      url2 = url.replaceFirst('/' + contextPath, "");
      logger.debug("stripped context: " + url);
    }
    url2
  }

  protected def rootCommunity = jc.getCommunityManager.getRootCommunity


  /**
   * Method takes a uri tokens, and tries to find a matching object.
   *
   * This method will be called recursively until it find the correct match.
   * A standard initial call to it may look something like this:
   * findObjectFromUriTokens(tokens(uri), rootCommunity)
   *
   * Which would mean start from the rootCommunity and use the current uri portion tokenized.
   *
   * Note that this method expects that you stripped off the /community or /spaces
   *
   * Uri Tokens:
   * If the uri is something like /one/two/three then the tokens will be List("one", "two", "three").
   *
   * @param tokens Tokens matching the uri
   * @param tokens The jive object at the current context.
   */
  protected[milton] def findObjectFromUriTokens(tokens: List[String], j: JiveObject): Option[JiveObject] = {

    def documentFromCommunity(s: String, c: Community): Option[Document] = {
      logger.info("documentFromCommunity s: " + s + " c: " + c)
      childDocuments(c).find{
        d => d.getBinaryBody.getName == s
      } match {
        case None => None
        case Some(d) => Some(d)
      }
    }

    def matchingCommunity(c: Community, name: String): Option[Community] = {
      logger.info("matchingCommunity c: " + c.getName + " name: " + name)
      // todo, caching or something more efficient here
      childCommunities(c).find(c => c.getName == name) match {
        case Some(x) => Some(x)
        case None => None
      }
    }

    j match {
    // go until we find a document, even if there are more tokens (we will ignore them)
      case d: Document => Some(d)
      case c: Community =>
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
                      case Nil => Some(c) // return the community we are out of tokens.
                      case _ => findObjectFromUriTokens(tail, c)
                    }
                }
            }

        }
    }
  }

  protected def printList(list: Array[String]): String = list match {
    case null => null
    case _ => printList(list.toList)
  }

  protected def printList(list: List[String]): String = list match {
    case Nil => ""
    case _ => "[" + list.reduceLeft(_ + ", " + _) + "]"
  }

  protected def tokens(uri: String) = {
    val list: List[String] = uri.split("/").toList
    list.filter{
      case "" => false;
      case _ => true
    }
  }

  protected val childCommunities = WebdavUtil.childCommunities(_: Community, jc.getCommunityManager)
  protected val childDocuments = WebdavUtil.childDocuments(_: Community, jc.getDocumentManager)

}