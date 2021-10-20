package io.zell.zdb.state.raw

import io.camunda.zeebe.db.ColumnFamily
import io.camunda.zeebe.db.TransactionContext
import io.camunda.zeebe.db.ZeebeDb
import io.camunda.zeebe.db.impl.DbLong
import io.camunda.zeebe.engine.state.ZbColumnFamilies
import io.camunda.zeebe.engine.state.instance.ElementInstance
import io.camunda.zeebe.protocol.impl.record.value.processinstance.ProcessInstanceRecord
import io.camunda.zeebe.protocol.record.intent.ProcessInstanceIntent

class ElementInstanceKeyColumnFamily(
    zeebeDb: ZeebeDb<ZbColumnFamilies>,
    transactionContext: TransactionContext
) {

    private val elementInstanceKey: DbLong = DbLong()
    private var elementInstance: ElementInstance =
        ElementInstance(-1, ProcessInstanceIntent.ACTIVATE_ELEMENT, ProcessInstanceRecord())
    private var elementInstanceColumnFamily: ColumnFamily<DbLong, ElementInstance>


    init {
        elementInstanceColumnFamily = zeebeDb.createColumnFamily(
            ZbColumnFamilies.ELEMENT_INSTANCE_KEY,
            transactionContext,
            elementInstanceKey,
            elementInstance
        )
    }

    fun acceptWhileTrue(visitor: Visitor) {
        elementInstanceColumnFamily.whileTrue { elementInstanceKey, elementInstance ->
            visitor.visit(
                ElementInstanceKeyEntry(
                    elementInstanceKey.value,
                    copyElementInstance(elementInstance)
                )
            )
        }
    }

    private fun copyElementInstance(elementInstance: ElementInstance): ElementInstance {

        val parent = get(elementInstance.parentKey)?.elementInstance

        val result = ElementInstance(
            elementInstance.key,
            parent,
            elementInstance.state,
            elementInstance.value
        )

        //TODO copy other fields
        result.jobKey = elementInstance.jobKey
        //...

        return result
    }

    fun get(key: Long): ElementInstanceKeyEntry? {
        elementInstanceKey.wrapLong(key)

        val elementInstance = elementInstanceColumnFamily.get(elementInstanceKey)

        return if (elementInstance == null) {
            null
        } else {
            ElementInstanceKeyEntry(key, copyElementInstance(elementInstance))
        }
    }

    fun findOrphans(): Collection<ElementInstanceKeyOrphanEntry> {
        val orphans = mutableListOf<ElementInstanceKeyOrphanEntry>()
        elementInstanceColumnFamily.forEach { elementInstanceKey, elementInstance ->

            val parentKey = elementInstance.parentKey

            if (parentKey != -1L) {
                val parent = get(parentKey)
                if (parent == null) {
                    orphans.add(
                        ElementInstanceKeyOrphanEntry(
                            elementInstanceKey.value,
                            copyElementInstance(elementInstance),
                            parentKey
                        )
                    )
                }
            }

        }
        return orphans
    }

    data class ElementInstanceKeyEntry(
        val elementInstanceKey: Long,
        val elementInstance: ElementInstance
    )

    /**
     * Represents an orphaned entry, i.e. one that points to a parent, but the parent is gone
     */
    data class ElementInstanceKeyOrphanEntry(
        val elementInstanceKey: Long,
        val elementInstance: ElementInstance,
        val parentKey: Long
    )


    fun interface Visitor {
        fun visit(entry: ElementInstanceKeyEntry): Boolean
    }

}