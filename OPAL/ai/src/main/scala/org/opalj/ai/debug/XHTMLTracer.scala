/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package ai
package debug

import scala.language.existentials

import org.opalj.util.writeAndOpen
import org.opalj.br.Code
import org.opalj.br.instructions.CHECKCAST
import org.opalj.br.instructions.FieldAccess
import org.opalj.br.instructions.Instruction
import org.opalj.br.instructions.LoadString
import org.opalj.br.instructions.NEW
import org.opalj.br.instructions.NonVirtualMethodInvocationInstruction
import org.opalj.ai.debug.XHTML.dumpLocals
import org.opalj.ai.debug.XHTML.dumpStack

case class FlowEntity(
        pc: PC,
        instruction: Instruction,
        operands: Operands[_ >: Null <: Domain#DomainValue],
        locals: Locals[_ >: Null <: Domain#DomainValue],
        properties: Option[String]) {
    val flowId = FlowEntity.nextFlowId
}

private[ai] object FlowEntity {
    private var flowId = -1
    private def nextFlowId = { flowId += 1; flowId }
    def lastFlowId = flowId - 1
}

/**
 * A tracer that generates an HTML document.
 *
 * @author Michael Eichberg
 */
trait XHTMLTracer extends AITracer {

    private[this] var flow: List[List[FlowEntity]] = List(List.empty)
    private[this] def newBranch(): List[List[FlowEntity]] = {
        flow = List.empty[FlowEntity] :: flow
        flow
    }
    private[this] def addFlowEntity(flowEntity: FlowEntity) {
        if (flow.head.exists(_.pc == flowEntity.pc))
            newBranch()

        flow = (flowEntity :: flow.head) :: flow.tail
    }

    private def instructionToNode(
        flowId: Int,
        pc: PC,
        instruction: Instruction): xml.Node = {
        val openDialog = "$( \"#dialog"+flowId+"\" ).dialog(\"open\");"
        val instructionAsString =
            instruction match {
                case NEW(objectType) ⇒
                    "new …"+objectType.simpleName;
                case CHECKCAST(referenceType) ⇒
                    "checkcast "+referenceType.toJava;
                case LoadString(s) if s.size < 5 ⇒
                    "Load \""+s+"\"";
                case LoadString(s) ⇒
                    "Load \""+s.substring(0, 4)+"…\""
                case fieldAccess: FieldAccess ⇒
                    fieldAccess.mnemonic+" "+fieldAccess.name
                case invoke: NonVirtualMethodInvocationInstruction ⇒
                    val declaringClass = invoke.declaringClass.toJava
                    "…"+declaringClass.substring(declaringClass.lastIndexOf('.') + 1)+" "+
                        invoke.name+"(…)"
                case _ ⇒ instruction.toString(pc)
            }

        <span onclick={ openDialog } title={ instruction.toString(pc) }>
            { instructionAsString }
        </span>
    }

    def dumpXHTML(title: String): scala.xml.Node = {
        import scala.collection.immutable.{ SortedMap, SortedSet }

        val inOrderFlow = flow.map(_.reverse).reverse
        var pathsCount = 0
        var pcs = SortedSet.empty[PC]
        for (path ← flow) {
            pathsCount += 1
            for (entity ← path) {
                pcs += entity.pc
            }
        }
        val pcsToRowIndex = SortedMap.empty[Int, Int] ++ pcs.zipWithIndex
        val ids = new java.util.IdentityHashMap[AnyRef, Integer]
        var nextId = 1
        val idsLookup = (value: AnyRef) ⇒ {
            var id = ids.get(value)
            if (id == null) {
                id = nextId
                nextId += 1
                ids.put(value, id)
            }
            id.intValue()
        }
        val dialogSetup =
            for (path ← inOrderFlow; entity ← path) yield {
                xml.Unparsed("$(function() { $( \"#dialog"+entity.flowId+"\" ).dialog({autoOpen:false}); });\n")
            }
        val dialogs =
            for {
                pathWithColumnIndex ← inOrderFlow.zipWithIndex
                (path, index) = pathWithColumnIndex
                flowEntity ← path
            } yield {
                val dialogId = "dialog"+flowEntity.flowId
                <div id={ dialogId } title={ (index + 1)+" - "+flowEntity.pc+" ("+flowEntity.instruction.mnemonic+")" }>
                    <b>Stack</b><br/>
                    { dumpStack(flowEntity.operands)(Some(idsLookup)) }
                    <b>Locals</b><br/>
                    { dumpLocals(flowEntity.locals)(Some(idsLookup)) }
                </div>
            }
        def row(pc: PC) =
            for (path ← inOrderFlow) yield {
                val flowEntity = path.find(_.pc == pc)
                <td>
                    { flowEntity.map(fe ⇒ instructionToNode(fe.flowId, pc, fe.instruction)).getOrElse(xml.Text(" ")) }
                </td>
            }
        val joinInstructions = code.joinInstructions
        val flowTable =
            for ((pc, rowIndex) ← pcsToRowIndex) yield {
                <tr>
                    <td>{ if (joinInstructions.contains(pc)) "⇶ " else "" } <b>{ pc }</b></td>
                    { row(pc) }
                </tr>
            }

        <html lang="en">
            <head>
                <meta charset="utf-8"/>
                <title>{ title+" (Paths: "+pathsCount+"; Flow Nodes: "+FlowEntity.lastFlowId+")" }</title>
                <link rel="stylesheet" href="http://code.jquery.com/ui/1.10.3/themes/smoothness/jquery-ui.css"/>
                <script src="http://code.jquery.com/jquery-1.9.1.js"></script>
                <script src="http://code.jquery.com/ui/1.10.3/jquery-ui.js"></script>
                <script>
                    { dialogSetup }
                </script>
                <style>
                    table {{
			width:100%;
			font-size: 12px;
			font-family: Tahoma;
			margin: 0px;
        	padding: 0px;
			border: 1px solid gray;
        	border-collapse: collapse;
		}}
        tr {{ 
        	margin: 0px;
        	padding: 0px;
        	border: 0px:
        }}
        td {{
        	margin: 0px;	
        	padding: 0px;
        	border: 0px;
        }}
        td ~ td {{
        	border-right: 1px solid #999;
        }}
        td.hovered {{  
        	background-color: lightblue;  
        	color: #666;  
        }}
        /*
        ui-dialog: The outer container of the dialog.
        ui-dialog-titlebar: The title bar containing the dialog's title and close button.
        ui-dialog-title: The container around the textual title of the dialog.
        ui-dialog-titlebar-close: The dialog's close button.
        ui-dialog-content: The container around the dialog's content. This is also the element the widget was instantiated with.
        ui-dialog-buttonpane: The pane that contains the dialog's buttons. This will only be present if the buttons option is set.
        ui-dialog-buttonset: The container around the buttons themselves.
        */
        .ui-dialog-content {{
        	font-family: Tahoma,Arial;
        	font-size: 11px; 
        }}
                </style>
            </head>
            <body style="font-family:Tahoma;font-size:8px;">
                <label for="filter">Filter</label>
                <input type="text" name="filter" value="" id="filter" title="Use a RegExp to filter(remove) elements. E.g.,'DUP|ASTORE|ALOAD'"/>
                <table>
                    <thead><tr>
                               <td>PC&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td>
                               { (1 to inOrderFlow.size).map(index ⇒ <td>{ index }</td>) }
                           </tr></thead>
                    <tbody>
                        { flowTable }
                    </tbody>
                </table>
                { dialogs }
                <script>
                    $('tbody tr').hover(function(){{  
        	$(this).find('td').addClass('hovered');  
        }}, function(){{  
        	$(this).find('td').removeClass('hovered');  
        }});
        function filter(selector, query) {{  
        	$(selector).each(function() {{  
        		($(this).text().search(new RegExp(query, 'i')){ xml.Unparsed("<") }
                    0) ? $(this).show().addClass('visible') : $(this).hide().removeClass('visible');  
        	}});  
        }};
        //default each row to visible  
        $('tbody tr').addClass('visible');  
    
        $('#filter').keyup(function(event) {{  
        		//if esc is pressed or nothing is entered  
        		if (event.keyCode == 27 || $(this).val() == '') {{  
        			//if esc is pressed we want to clear the value of search box  
        			$(this).val('');  
        			//we want each row to be visible because if nothing  
        			//is entered then all rows are matched.  
        			$('tbody tr').removeClass('visible').show().addClass('visible');  
        		}}  
        		//if there is text, lets filter  
        		else {{  
        			filter('tbody tr', $(this).val());  
        		}} 
        }});
                </script>
            </body>
        </html>
    }

    private var code: Code = null

    def continuingInterpretation(
        code: Code,
        domain: Domain)(
            initialWorkList: List[PC],
            alreadyEvaluated: List[PC],
            operandsArray: domain.OperandsArray,
            localsArray: domain.LocalsArray,
            memoryLayoutBeforeSubroutineCall: List[(domain.OperandsArray, domain.LocalsArray)]) {
        if ((this.code eq code) || (this.code == null))
            this.code = code
        else
            throw new IllegalStateException("this XHTMLtracer is already used; create a new one")

    }

    private[this] var continuingWithBranch = true

    def flow(
        domain: Domain)(
            currentPC: PC,
            successorPC: PC,
            isExceptionalControlFlow: Boolean) = {
        continuingWithBranch = currentPC < successorPC
    }

    def noFlow(domain: Domain)(currentPC: PC, targetPC: PC): Unit = { /*EMPTY*/ }

    def rescheduled(
        domain: Domain)(
            sourcePC: PC,
            targetPC: PC,
            isExceptionalControlFlow: Boolean): Unit = {
        /*ignored for now*/
    }

    def instructionEvalution(
        domain: Domain)(
            pc: PC,
            instruction: Instruction,
            operands: domain.Operands,
            locals: domain.Locals): Unit = {
        if (!continuingWithBranch)
            newBranch()

        addFlowEntity(
            FlowEntity(
                pc,
                instruction,
                operands,
                locals,
                domain.properties(pc)))
        // if we have a call to instruction evaluation without an intermediate
        // flow call, we are continuing the evaluation with a branch
        continuingWithBranch = false
    }

    def join(
        domain: Domain)(
            pc: PC,
            thisOperands: domain.Operands,
            thisLocals: domain.Locals,
            otherOperands: domain.Operands,
            otherLocals: domain.Locals,
            result: Update[(domain.Operands, domain.Locals)]): Unit = { /*ignored*/ }

    def establishedConstraint(
        domain: Domain)(
            pc: PC,
            effectivePC: PC,
            operands: domain.Operands,
            locals: domain.Locals,
            newOperands: domain.Operands,
            newLocals: domain.Locals): Unit = { /*ignored*/ }

    def abruptMethodExecution(
        domain: Domain)(
            pc: Int,
            exception: domain.DomainValue): Unit = { /*ignored*/ }

    def jumpToSubroutine(
        domain: Domain)(
            pc: PC, target: PC, nestingLevel: Int): Unit = { /* ignored */ }

    def returnFromSubroutine(
        domain: Domain)(
            pc: PC,
            returnAddress: PC,
            subroutineInstructions: List[PC]): Unit = { /*ignored*/ }

    /**
     * Called when a ret instruction is encountered.
     */
    def ret(
        domain: Domain)(
            pc: PC,
            returnAddress: PC,
            oldWorklist: List[PC],
            newWorklist: List[PC]): Unit = { /*ignored*/ }

    def domainMessage(
        domain: Domain,
        source: Class[_], typeID: String,
        pc: Option[PC], message: ⇒ String): Unit = { /*EMPTY*/ }

    def result(result: AIResult): Unit = {
        writeAndOpen(
            dumpXHTML((new java.util.Date).toString()),
            "TraceOfAbstractInterpretation",
            ".html"
        )
    }

}

class ConfigurableXHTMLTracer(openDumpOnResult: Boolean = false) extends XHTMLTracer {

    override def result(result: AIResult): Unit = {
        if (openDumpOnResult)
            super.result(result)
    }

}

