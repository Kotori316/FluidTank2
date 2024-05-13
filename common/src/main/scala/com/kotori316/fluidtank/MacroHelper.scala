package com.kotori316.fluidtank

import scala.annotation.tailrec
import scala.quoted.*

object MacroHelper {
  /**
   * @return the name of method where the caller belongs
   */
  inline def getCallerMethod: String = ${ getCallerMethodImpl() }

  private def getCallerMethodImpl()(using quotes: Quotes): Expr[String] = {
    import quotes.reflect.{*, given}

    @tailrec
    def enclosingMethod(symbol: Symbol): Symbol = {
      symbol match {
        // search parent if symbol is lambda
        case sym if sym.flags `is` (Flags.Artifact | Flags.Method | Flags.Synthetic) => enclosingMethod(sym.owner)
        case sym if sym.flags `is` Flags.Method => sym
        case _ => enclosingMethod(symbol.owner)
      }
    }

    val callee = enclosingMethod(Symbol.spliceOwner)
    Expr(callee.name)
  }
}
