package kamon.instrumentation.futures.cats

import java.util.concurrent.Executors

import kamon.Kamon
import kamon.tag.Lookups.plain
import kamon.context.Context
import org.scalatest.{Matchers, OptionValues, WordSpec}
import org.scalatest.concurrent.{Eventually, PatienceConfiguration, ScalaFutures}

import scala.concurrent.ExecutionContext.global
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import monix.eval.Task
import monix.execution.Scheduler

class MonixInstrumentationSpec extends WordSpec with ScalaFutures with Matchers with PatienceConfiguration
    with OptionValues with Eventually {

  // NOTE: We have this test just to ensure that the Context propagation is working, but starting with Kamon 2.0 there
  //       is no need to have explicit Runnable/Callable instrumentation because the instrumentation brought by the
  //       kamon-executors module should take care of all non-JDK Runnable/Callable implementations.

  "an Monix Task created when instrumentation is active" should {
    "capture the active span available when created" which {
      "must be available across asynchronous boundaries" in {
        implicit val ctxShift: Scheduler = Scheduler(global)

        val anotherExecutionContext: ExecutionContext =
          ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())
        val context = Context.of("key", "value")
        val contextTagAfterTransformations =
          for {
            scope <- Task {
              Kamon.storeContext(context)
            }
            len <- Task("Hello Kamon!").map(_.length)
            _ <- Task(len.toString)
            _ <- Task.shift(global)
            _ <- Task.shift
            _ <- Task.shift(anotherExecutionContext)
          } yield {
            val tagValue = Kamon.currentContext().getTag(plain("key"))
            scope.close()
            tagValue
          }

        val contextTagFuture = contextTagAfterTransformations.runToFuture


        eventually(timeout(10 seconds)) {
          contextTagFuture.value.get.get shouldBe "value"
        }
      }

      "must be available in parallel settings" in {
        implicit val ctxShift: Scheduler = monix.execution.Scheduler.Implicits.global
        val context = Context.of("key", "value")

        val anotherExecutionContext: ExecutionContext =
          ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())

        val threads = (0 to 5).toList
        val parallel = threads.map(_ => Task {
          Kamon.currentContext().getTag(plain("key"))
         })

        val contextTagAfterTransformations =
          for {
            scope <- Task {
              Kamon.storeContext(context)
            }
            len <- Task("Hello Kamon!").map(_.length)
            _ <- Task(len.toString)
            _ <- Task.shift(global)
            _ <- Task.shift
            _ <- Task.shift(anotherExecutionContext)
            tagValues <- Task.gatherUnordered(parallel)
          } yield {
            scope.close()
            tagValues
          }

        val contextTagFuture = contextTagAfterTransformations.runToFuture


        eventually(timeout(10 seconds)) {
          contextTagFuture.value.get.get shouldBe threads.map(_ => "value")
        }
      }
    }
  }
}