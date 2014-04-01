package quickstart

import org.apache.uima.UIMAFramework
import org.apache.uima.util.XMLInputSource
import org.apache.uima.cas.impl.XmiCasDeserializer
import org.apache.uima.util.CasCreationUtils
import org.apache.uima.resource.metadata.impl.TypePriorities_impl

import de.fraunhofer.scai.bio.uima.core.util.UIMAViewUtils
import de.fraunhofer.scai.bio.uima.core.util.UIMATypeSystemUtils

import de.fraunhofer.scai.bio.extraction.types.text.Section
import de.fraunhofer.scai.bio.extraction.types.text.CoreAnnotation
import de.fraunhofer.scai.bio.extraction.types.image.ImageAnnotation

object UIMAMain {
  def main(args: Array[String]) {
    val tsFile = new XMLInputSource(getClass.getResource("/SCAITypeSystem.xml"))
    val idxFile = new XMLInputSource(getClass.getResource("/SCAIIndexCollectionDescriptor.xml"))

    val typesystem = UIMAFramework.getXMLParser().parseTypeSystemDescription(
            tsFile);
    val indexes = UIMAFramework.getXMLParser().parseFsIndexCollection(idxFile)
            .getFsIndexes();

    val cas = CasCreationUtils.createCas(typesystem, new TypePriorities_impl(), indexes)
    cas.reset

    val document = getClass.getResourceAsStream("/US2732300A.pdf.xmi")

    XmiCasDeserializer.deserialize(document, cas)

    val doc = UIMAViewUtils.getOrCreatePreferredView(cas.getJCas, "DocumentView")
    // UIMAViewUtils.showAllViews(cas.getJCas)

    val sectionType = UIMATypeSystemUtils.getWantedType(doc, "Section", "de.fraunhofer.scai.bio.extraction.types.text")
    val annotationIndex = doc.getAnnotationIndex(sectionType)
    val it = annotationIndex.iterator()
    
    while(it.hasNext) {
      val annotation = it.next.asInstanceOf[CoreAnnotation]
      if (annotation != null) {
        if (annotation.isInstanceOf[Section]) {
          val sec = annotation.asInstanceOf[Section]
          println(sec.getConcept.getIdentifier)
          println(sec.getCoveredText)
        }
      }
    }

    tsFile.close
    idxFile.close
    document.close
  }
}


import xitrum.Server
import xitrum.Config
import xitrum.WebSocketAction
import xitrum.annotation.WEBSOCKET
import xitrum.{WebSocketText, WebSocketBinary, WebSocketPing, WebSocketPong}

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props

import com.github.pathikrit.dijon

import de.fraunhofer.scai.scaltex.ast._

object UIMAwithActors {
  class UpdaterActor extends Actor {
    var websockets = Set[ActorRef]()

    def receive = {
      case Register(ref) => websockets += ref
      case Deregister(ref) => websockets -= ref
      case x => if (websockets.size > 0) websockets.map(_ ! x)
    }
  }

  def prepareActors {
    Factory.system = Config.actorSystem
    Factory.updater = Config.actorSystem.actorOf(Props[UpdaterActor], "updater")

    val tsFile = new XMLInputSource(getClass.getResource("/SCAITypeSystem.xml"))
    val idxFile = new XMLInputSource(getClass.getResource("/SCAIIndexCollectionDescriptor.xml"))

    val typesystem = UIMAFramework.getXMLParser().parseTypeSystemDescription(
            tsFile);
    val indexes = UIMAFramework.getXMLParser().parseFsIndexCollection(idxFile)
            .getFsIndexes();

    val cas = CasCreationUtils.createCas(typesystem, new TypePriorities_impl(), indexes)
    cas.reset

    val document = getClass.getResourceAsStream("/US2732300A.pdf.xmi")

    XmiCasDeserializer.deserialize(document, cas)

    val doc = UIMAViewUtils.getOrCreatePreferredView(cas.getJCas, "DocumentView")
    // UIMAViewUtils.showAllViews(cas.getJCas)

    val sectionType = UIMATypeSystemUtils.getWantedType(doc, "Section", "de.fraunhofer.scai.bio.extraction.types.text")
    val annotationIndex = doc.getAnnotationIndex(sectionType)
    val it = annotationIndex.iterator()
    
    while(it.hasNext) {
      val annotation = it.next
      if (annotation != null) {
        if (annotation.isInstanceOf[Section]) {
          val sec = annotation.asInstanceOf[Section]
          val node1 = Factory.makeEntityActor[EntityActor]
          node1 ! Msg.Content(sec.getConcept.getIdentifier)
          node1 ! Msg.ClassDef("Section")
          val node2 = Factory.makeEntityActor[EntityActor]
          node2 ! Msg.Content(sec.getCoveredText)
          node2 ! Msg.ClassDef("Text")
        }
      }
    }

    // get image
    import javax.imageio.ImageIO
    import org.apache.uima.cas.{ByteArrayFS, ArrayFS}
    import java.io.ByteArrayInputStream
    val img = UIMAViewUtils.getOrCreatePreferredView(cas.getJCas, "ImageView")
    val something = img.getSofaDataArray.asInstanceOf[ArrayFS].get(0).asInstanceOf[ByteArrayFS]
    var buffer = new Array[Byte](something.size)
    something.copyToArray(0, buffer, 0, something.size)
    val bufferedImage = ImageIO.read(new ByteArrayInputStream(buffer))
    ImageIO.write(bufferedImage, "png", new java.io.File("public/tmp/Page-0.png"))

    val node = Factory.makeEntityActor[EntityActor]
    node ! Msg.Content(s"""url = "tmp/Page-0.png",\ndesc = "Deckblatt" """)
    node ! Msg.ClassDef("Figure")

    tsFile.close
    idxFile.close
    document.close

  }

  def main(args: Array[String]) {
    prepareActors
    Server.start()
  }
}