package com.billmii.android.ui.classification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billmii.android.data.model.*
import com.billmii.android.data.service.ClassificationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Classification Rule Management
 */
@HiltViewModel
class ClassificationRuleViewModel @Inject constructor(
    private val classificationService: ClassificationService
) : ViewModel() {
    
    private val _rules = MutableStateFlow<List<ClassificationRuleUI>>(emptyList())
    val rules: StateFlow<List<ClassificationRuleUI>> = _rules.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog.asStateFlow()
    
    private val _showEditDialog = MutableStateFlow(false)
    val showEditDialog: StateFlow<Boolean> = _showEditDialog.asStateFlow()
    
    var selectedRule: ClassificationRuleUI? = null
        private set
    
    init {
        loadRules()
    }
    
    private fun loadRules() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                classificationService.getEnabledRules()
                    .collect { rulesList ->
                        _rules.value = rulesList.map { it.toUIModel() }
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun showCreateRuleDialog() {
        _showCreateDialog.value = true
    }
    
    fun hideCreateDialog() {
        _showCreateDialog.value = false
    }
    
    fun showEditRuleDialog(rule: ClassificationRuleUI) {
        selectedRule = rule
        _showEditDialog.value = true
    }
    
    fun hideEditDialog() {
        _showEditDialog.value = false
        selectedRule = null
    }
    
    fun createRule(
        name: String,
        description: String?,
        conditions: List<ConditionUI>,
        actions: List<ActionUI>
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val rule = ClassificationRule(
                    name = name,
                    description = description,
                    conditions = conditions.map { it.toModel() },
                    actions = actions.map { it.toModel() },
                    enabled = true,
                    priority = _rules.value.size
                )
                classificationService.createRule(rule)
                loadRules()
                hideCreateDialog()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun updateRule(rule: ClassificationRuleUI) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val modelRule = ClassificationRule(
                    id = rule.id,
                    name = rule.name,
                    description = rule.description,
                    conditions = rule.conditions.map { it.toModel() },
                    actions = rule.actions.map { it.toModel() },
                    enabled = rule.enabled,
                    priority = rule.priority
                )
                classificationService.updateRule(modelRule)
                loadRules()
                hideEditDialog()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun deleteRule(rule: ClassificationRuleUI) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val modelRule = ClassificationRule(
                    id = rule.id,
                    name = rule.name,
                    description = rule.description,
                    conditions = rule.conditions.map { it.toModel() },
                    actions = rule.actions.map { it.toModel() },
                    enabled = rule.enabled,
                    priority = rule.priority
                )
                classificationService.deleteRule(modelRule)
                loadRules()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun toggleRule(ruleId: Long) {
        viewModelScope.launch {
            try {
                classificationService.toggleRule(ruleId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun moveRule(ruleId: Long, newPriority: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentRules = _rules.value.toMutableList()
                val movingRule = currentRules.find { it.id == ruleId } ?: return@launch
                
                val oldPriority = movingRule.priority
                currentRules[oldPriority] = currentRules[newPriority].copy(priority = oldPriority)
                currentRules[newPriority] = movingRule.copy(priority = newPriority)
                
                val ruleIds = currentRules.map { it.id }
                classificationService.reorderRules(ruleIds)
                loadRules()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}

// Extension functions
private fun ClassificationRule.toUIModel(): ClassificationRuleUI {
    return ClassificationRuleUI(
        id = id,
        name = name,
        description = description,
        conditions = conditions.map { it.toUIModel() },
        actions = actions.map { it.toUIModel() },
        enabled = enabled,
        priority = priority
    )
}

private fun ClassificationCondition.toUIModel(): ConditionUI {
    return ConditionUI(field, operator, value)
}

private fun ClassificationAction.toUIModel(): ActionUI {
    return ActionUI(type, value)
}

private fun ConditionUI.toModel(): ClassificationCondition {
    return ClassificationCondition(field, operator, value)
}

private fun ActionUI.toModel(): ClassificationAction {
    return ClassificationAction(type, value)
}