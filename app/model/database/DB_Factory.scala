package model.database

import java.io.FileInputStream
import java.sql.{ResultSet, Statement}
import java.util.Properties

/**
 * Factory
 */
object DB_Factory {

  private val logger = play.api.Logger(this.getClass)

  /**
   * Creates a connection to a configured database using url in app.properties.
   * Currently hardcoded to a postgresql db, should eventually implement other types
   *
   * Is called in the withDB function
   *
   * @param url: String, connection url for database
   * @return : an instance of a DB_Interface, for now just returns a postgresql obj
   *
   *         TODO: implement more db types like mongo, mysql, etc
   */
  def createDB (url: String): DB_Interface = {
    new DB_PostgreS(url)
  }

  /**
   * Main access point for DB usage across the entire application. Any and all
   * database calls will be done through this function. Automatically creates a DB connection
   * to the specified type of DB (currently hardcoded to postgresql) and then executes the operation
   * specified in the lambda. These are locked to being called methods of some subclass of DB_Interface
   *
   * @param func: (+DB_Interface, Statement) => Option[ResultSet], The main bread and butter of this function.
   *              this lambda will be what withDB executes, which it will call from whatever DB_Interface subclass
   *              is being made.
   * @tparam T: Not really used atm, might remove
   * @return : Option[ResultSet], more often than not just returns the result of whatever operation was called
   */
  def withDB [T] (func: (DB_Interface, Statement) => Option[ResultSet]): Option[ResultSet] = {

    logger.info("starting Database operation")
    val startTime = System.currentTimeMillis()

    // initialize database properties
    val prop = new Properties();
    prop.load(new FileInputStream("./app.properties"))
    val temp = prop.getProperty("DB_URL")

    // actually create DB connection
    val db = DB_Factory.createDB (
      temp
      //NOTE: this will not work unless you have a file called app.properties with a working db url in it, attributed to a property called DB_URL
    )

    // db value above and statement value here used in lambda, could maybe just leave it in here so I don't have to
    // call (db, statement) every fucking time but idk I'm lazy and if it works it works.
    val statement = db.openState()

    // final error checking and return logic
    try {
      val res = func (db, statement)
      if (res.nonEmpty) res
      else None
    } catch {
      case e:Exception => println(e)
        logger.info(s"Database operation terminated unexpectedly, took ${System.currentTimeMillis()-startTime}ms\n\n")
        throw new RuntimeException(e)
    } finally {
      logger.info(s"Database operation completed, total took ${System.currentTimeMillis()-startTime}ms\n\n")
      db.close()
    }
  }
}
