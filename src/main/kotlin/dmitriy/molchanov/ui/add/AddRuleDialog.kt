package dmitriy.molchanov.ui.add

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import dmitriy.molchanov.Strings
import dmitriy.molchanov.model.Rule
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.*

/**
 * Дилог сохранения/редактирования правила
 *
 * @param editablePrefix редактируемое правило. Если null, значит создаем новое.
 * @param gitRepUrls список активных Git репозиториев (репозиториев открытых проектов).
 * @property getCurBranchByUrl функция получения текущей ветки по выбранному репозиторию.
 */
class AddRuleDialog(
    editablePrefix: Rule? = null,
    gitRepUrls: List<String>,
    private val getCurBranchByUrl: (String) -> String?
) : DialogWrapper(true) {

    val rule: Rule?
        get() = selectedGitRep?.let { Rule(it, prefixEdit.text, checkStringEdit.text) }

    private val selectedGitRep: String?
        get() = gitRepBox.selectedItem?.toString()
    private val gitRepBox = ComboBox(gitRepUrls.toTypedArray())
    private val prefixEdit = JTextField(TEXT_COLUMNS)
    private val checkStringEdit = JTextField(TEXT_COLUMNS)
    private val statusLabel = JLabel(Strings.FILL_FIELDS)

    private val keyListener = object : KeyListener {
        override fun keyTyped(p0: KeyEvent?) {}
        override fun keyPressed(p0: KeyEvent?) {}
        override fun keyReleased(p0: KeyEvent?) = updateDialogStatus()
    }

    init {
        init()
        gitRepBox.isEditable = true
        gitRepBox.addActionListener { updateCheckString() }
        title = Strings.ADD_RULE
        editablePrefix?.gitRepo?.let(gitRepBox::setToolTipText)
        editablePrefix?.regexPrefix?.let(prefixEdit::setText)
        editablePrefix?.checkString
                ?.let(checkStringEdit::setText)
                ?: updateCheckString()
        updateDialogStatus()
    }

    override fun createCenterPanel(): JComponent {
        // Создание панели для размещение компонентов
        val root = BoxLayoutUtils.createVerticalPanel()
        val getRepLabel = JLabel(Strings.GIT_REPO)
        val prefixLabel = JLabel(Strings.REGEX_PREFIX)
        val checkStringLabel = JLabel(Strings.CHECK_BRANCH)
        val gitRepGroup = getViewGroup(getRepLabel, gitRepBox)
        val prefixGroup = getViewGroup(prefixLabel, prefixEdit)
        val statusGroup = getCheckText(statusLabel)
        val checkStringGroup = getViewGroup(checkStringLabel, checkStringEdit)

        // Определение размеров надписей к текстовым полям
        GuiUtils.makeSameSize(arrayOf(getRepLabel, prefixLabel, checkStringLabel))

        root.add(gitRepGroup)
        root.add(prefixGroup)
        root.add(checkStringGroup)
        root.add(statusGroup)
        return root
    }

    /** Берем текущую ветку из выбранного репозитория и вставляем в [checkStringEdit] */
    private fun updateCheckString() {
        selectedGitRep
                ?.let(getCurBranchByUrl)
                ?.let(checkStringEdit::setText)
    }

    private fun updateDialogStatus() {
        val dialogStatus = getDialogStatus()
        statusLabel.text = dialogStatus.message
        okAction.isEnabled = dialogStatus.shouldOkButtonActive
    }

    /** Получить строку вида "текст _ поле для ввода" */
    private fun getViewGroup(label: JLabel, component: JComponent): JPanel =
            BoxLayoutUtils.createHorizontalPanel().apply {
                component.addKeyListener(keyListener)
                add(label)
                add(Box.createHorizontalStrut(HORIZONTAL_STRUT))
                add(component)
            }

    private fun getCheckText(statusLabel: JLabel): JPanel =
            BoxLayoutUtils.createHorizontalPanel().apply {
                add(statusLabel)
            }

    /**
     * Получить статус заполнения формы
     */
    private fun getDialogStatus(): DialogStatus {
        var message = Strings.EMPTY
        if (selectedGitRep.isNullOrEmpty()) message += "${Strings.GIT_REPO_WARNING}, "
        if (prefixEdit.text.isNullOrEmpty()) message += "${Strings.REGEX_PREFIX_WARNING}, "
        if (checkStringEdit.text.isNullOrEmpty()) message += "${Strings.CHECK_BRANCH_WARNING}, "
        if (message.isNotEmpty()) {
            val showedMessage = message.substring(0, message.length - 2)
            return DialogStatus(
                    message = "<html><font color=red>$showedMessage</font></html>",
                    shouldOkButtonActive = false)
        }
        val regex = Regex(prefixEdit.text)
        val checkStr = checkStringEdit.text
        regex.find(checkStr)?.range?.apply {
            val lastMatch = if (last == checkStr.length) last else last + 1
            val startText = checkStr.substring(0, first)
            val matchText = checkStr.substring(first, lastMatch)
            val endText = checkStr.substring(lastMatch)
            return DialogStatus(
                    message = "<html>" +
                            "<font color=black>$startText</font>" +
                            "<font color=green>$matchText</font>" +
                            "<font color=black>$endText</font>" +
                            "</html>",
                    shouldOkButtonActive = true)
        }
        return DialogStatus(
                message = "<html><font color=red>${Strings.NO_MATCHES_FOUND}</font></html>",
                shouldOkButtonActive = false)
    }

    private class DialogStatus(val message: String, val shouldOkButtonActive: Boolean)

    private companion object {
        const val TEXT_COLUMNS = 15
        const val HORIZONTAL_STRUT = 10
    }
}