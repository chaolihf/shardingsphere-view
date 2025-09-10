/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.chinatelecom.udp.component.shardingsphere.postgresql;

import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.shardingsphere.sql.parser.api.visitor.format.SQLFormatVisitor;
import org.apache.shardingsphere.sql.parser.postgresql.visitor.statement.PostgreSQLStatementVisitor;

/**
 * SQL format visitor for PostgreSQL.
 */
public final class PostgreSQLFormatVisitor extends PostgreSQLStatementVisitor {

    private final StringBuilder formattedSQL = new StringBuilder(256);

    
    
    // @Override
    // public String getDatabaseType() {
    //     return "PostgreSQL";
    // }

    // @Override
    // public String visit(ParseTree tree) {
    //     StringBuilder sb = new StringBuilder();
    //     visitChildren(tree,sb);
    //     return sb.toString();
    // }

    // public void visitChildren(ParseTree node,StringBuilder output ) { 
    //     if (node instanceof TerminalNode){
    //         output.append(node.getText()).append(" ");
    //     } else{
    //         for(int i = 0; i < node.getChildCount(); i++){
    //             visitChildren(node.getChild(i),output);
    //         }
    //     }
    // }

    // @Override
    // public String visitChildren(RuleNode node) {
    //     throw new UnsupportedOperationException("Unimplemented method 'visitChildren'");
    // }

    // @Override
    // public String visitTerminal(TerminalNode node) {
    //     throw new UnsupportedOperationException("Unimplemented method 'visitTerminal'");
    // }

    // @Override
    // public String visitErrorNode(ErrorNode node) {
    //     throw new UnsupportedOperationException("Unimplemented method 'visitErrorNode'");
    // }
    
}
