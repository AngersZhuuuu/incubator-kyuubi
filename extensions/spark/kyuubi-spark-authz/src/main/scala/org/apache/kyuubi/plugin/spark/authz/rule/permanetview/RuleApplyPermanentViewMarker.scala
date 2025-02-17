/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.kyuubi.plugin.spark.authz.rule.permanetview

import org.apache.spark.sql.catalyst.expressions.SubqueryExpression
import org.apache.spark.sql.catalyst.plans.logical.{LogicalPlan, View}
import org.apache.spark.sql.catalyst.rules.Rule

import org.apache.kyuubi.plugin.spark.authz.rule.permanetview
import org.apache.kyuubi.plugin.spark.authz.util.AuthZUtils._

/**
 * Adding [[PermanentViewMarker]] for permanent views
 * for marking catalogTable of views used by privilege checking
 * in [[org.apache.kyuubi.plugin.spark.authz.ranger.RuleAuthorization]].
 * [[PermanentViewMarker]] must be transformed up later
 * in [[RuleEliminatePermanentViewMarker]] optimizer.
 */
class RuleApplyPermanentViewMarker extends Rule[LogicalPlan] {

  override def apply(plan: LogicalPlan): LogicalPlan = {
    plan mapChildren {
      case p: PermanentViewMarker => p
      case permanentView: View if hasResolvedPermanentView(permanentView) =>
        val resolvedSubquery = permanentView.transformAllExpressions {
          case subquery: SubqueryExpression =>
            subquery.withNewPlan(plan =
              permanetview.PermanentViewMarker(
                subquery.plan,
                permanentView.desc,
                permanentView.output.map(_.name)))
        }
        permanetview.PermanentViewMarker(
          resolvedSubquery,
          resolvedSubquery.desc,
          resolvedSubquery.output.map(_.name))
      case other => apply(other)
    }
  }
}
