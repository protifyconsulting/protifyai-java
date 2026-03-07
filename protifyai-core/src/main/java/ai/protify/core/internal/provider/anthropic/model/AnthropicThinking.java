/*
 * Copyright(c) 2026 Protify Consulting LLC. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package ai.protify.core.internal.provider.anthropic.model;

import ai.protify.core.internal.util.json.ProtifyJsonProperty;

public final class AnthropicThinking {

    private String type;

    @ProtifyJsonProperty("budget_tokens")
    private Integer budgetTokens;

    public AnthropicThinking() {
    }

    public AnthropicThinking(String type, Integer budgetTokens) {
        this.type = type;
        this.budgetTokens = budgetTokens;
    }

    public static AnthropicThinking enabled(int budgetTokens) {
        return new AnthropicThinking("enabled", budgetTokens);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @ProtifyJsonProperty("budget_tokens")
    public Integer getBudgetTokens() {
        return budgetTokens;
    }

    public void setBudgetTokens(Integer budgetTokens) {
        this.budgetTokens = budgetTokens;
    }
}
