package com.github.rbartczak05.stopideaplugin.toolWindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import java.awt.*
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.Timer

class WorkLifeBalanceWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val stopItToolWindow = StopItToolWindow(toolWindow)
        val content = ContentFactory.getInstance().createContent(stopItToolWindow.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    class StopItToolWindow(private val toolWindow: ToolWindow) {

        private val mainPanel = JBPanel<JBPanel<*>>(CardLayout())

        // Zmieniony układ na GridBagLayout, by wyśrodkować elementy bez ich rozciągania
        private val workPanel = JBPanel<JBPanel<*>>(GridBagLayout())
        private val breakPanel = JBPanel<JBPanel<*>>(BorderLayout())

        // Etykiety wyświetlające bieżący czas
        private val workTimeLabel = JBLabel("Czas pracy: 00:00", SwingConstants.CENTER)
        private val breakTimeLabel = JBLabel("Pozostały czas przerwy: 15:00", SwingConstants.CENTER)

        private val actionButton = JButton("Rozpocznij pracę")
        private var isWorking = false

        // Timery tykające co 1 sekundę
        private var workTimer: Timer? = null
        private var breakTimer: Timer? = null

        // Liczniki sekund
        private var workSeconds = 0
        private var breakSeconds = 15 * 60

        // Limit pracy przed automatyczną przerwą (1 godzina)
        private val WORK_LIMIT_SECONDS = 60 * 60

        // Elementy gry kółko i krzyżyk
        private val tttButtons = Array(3) { Array(3) { JButton("") } }

        init {
            setupWorkPanel()
            setupBreakPanel()

            mainPanel.add(workPanel, "WORK")
            mainPanel.add(breakPanel, "BREAK")
        }

        fun getContent() = mainPanel

        private fun formatTime(totalSeconds: Int): String {
            val m = (totalSeconds % 3600) / 60
            val s = totalSeconds % 60
            val h = totalSeconds / 3600
            return if (h > 0) String.format("%02d:%02d:%02d", h, m, s)
            else String.format("%02d:%02d", m, s)
        }

        private fun setupWorkPanel() {
            val gbc = GridBagConstraints()
            gbc.gridx = 0
            gbc.gridy = 0
            gbc.insets = Insets(10, 10, 20, 10) // Marginesy (góra, lewo, dół, prawo)

            workTimeLabel.font = workTimeLabel.font.deriveFont(Font.BOLD, 28f)
            workPanel.add(workTimeLabel, gbc)

            gbc.gridy = 1
            gbc.insets = Insets(0, 10, 10, 10)

            // Mniejsza czcionka w przycisku
            actionButton.font = actionButton.font.deriveFont(Font.BOLD, 14f)
            actionButton.addActionListener {
                if (isWorking) {
                    startBreak(isAutomatic = false)
                } else {
                    startWork()
                }
            }
            workPanel.add(actionButton, gbc)
        }

        private fun startWork() {
            isWorking = true
            actionButton.text = "Zakończ pracę i zrób przerwę"
            workSeconds = 0
            workTimeLabel.text = "Czas pracy: 00:00"

            workTimer = Timer(1000) {
                workSeconds++
                workTimeLabel.text = "Czas pracy: ${formatTime(workSeconds)}"

                if (workSeconds >= WORK_LIMIT_SECONDS) {
                    startBreak(isAutomatic = true)
                }
            }
            workTimer?.start()
        }

        private fun startBreak(isAutomatic: Boolean) {
            workTimer?.stop()
            isWorking = false
            actionButton.text = "Rozpocznij pracę"

            if (isAutomatic) {
                Messages.showInfoMessage(
                    toolWindow.project,
                    "Czas na przerwę! Pracowałeś/aś przez godzinę bez przerwy.",
                    "Czas Odpocząć"
                )
            }

            val cl = mainPanel.layout as CardLayout
            cl.show(mainPanel, "BREAK")
            resetGame()

            breakSeconds = 15 * 60
            breakTimeLabel.text = "Pozostały czas przerwy: ${formatTime(breakSeconds)}"

            breakTimer?.stop()
            breakTimer = Timer(1000) {
                breakSeconds--
                breakTimeLabel.text = "Pozostały czas przerwy: ${formatTime(breakSeconds)}"

                if (breakSeconds <= 0) {
                    endBreak(isAutomatic = true)
                }
            }
            breakTimer?.start()
        }

        private fun endBreak(isAutomatic: Boolean = false) {
            breakTimer?.stop()

            val cl = mainPanel.layout as CardLayout
            cl.show(mainPanel, "WORK")

            workSeconds = 0
            workTimeLabel.text = "Czas pracy: 00:00"

            if (isAutomatic) {
                Messages.showInfoMessage(
                    toolWindow.project,
                    "Przerwa zakończona. Możesz wrócić do pisania super kodu!",
                    "Koniec Przerwy"
                )
            }
        }

        private fun setupBreakPanel() {
            // Panel górny (napisy)
            val topPanel = JPanel()
            topPanel.layout = BoxLayout(topPanel, BoxLayout.Y_AXIS)
            topPanel.border = javax.swing.BorderFactory.createEmptyBorder(10, 0, 20, 0)

            breakTimeLabel.font = breakTimeLabel.font.deriveFont(Font.BOLD, 22f)
            breakTimeLabel.alignmentX = JPanel.CENTER_ALIGNMENT

            val infoLabel = JBLabel("W wolnej chwili się zrelaksuj...", SwingConstants.CENTER)
            infoLabel.font = infoLabel.font.deriveFont(Font.ITALIC, 14f)
            infoLabel.alignmentX = JPanel.CENTER_ALIGNMENT

            topPanel.add(breakTimeLabel)
            topPanel.add(javax.swing.Box.createVerticalStrut(10))
            topPanel.add(infoLabel)

            breakPanel.add(topPanel, BorderLayout.NORTH)

            // Panel środkowy (gra)
            // Dodano marginesy pomiędzy przyciskami (5px)
            val gridPanel = JPanel(GridLayout(3, 3, 5, 5))
            for (i in 0..2) {
                for (j in 0..2) {
                    val btn = tttButtons[i][j]
                    btn.font = btn.font.deriveFont(Font.BOLD, 32f)

                    // Wymuszamy kwadratowy, stały rozmiar przycisków
                    val size = Dimension(80, 80)
                    btn.preferredSize = size
                    btn.minimumSize = size
                    btn.maximumSize = size

                    btn.isFocusPainted = false
                    btn.addActionListener { onPlayerMove(i, j) }
                    gridPanel.add(btn)
                }
            }

            // Używamy GridBagLayout by wyśrodkować planszę i zapobiec jej rozciąganiu
            val gridWrapper = JPanel(GridBagLayout())
            gridWrapper.add(gridPanel)
            breakPanel.add(gridWrapper, BorderLayout.CENTER)

            // Panel dolny (Pomiń przerwę)
            val bottomPanel = JPanel(FlowLayout(FlowLayout.CENTER))
            bottomPanel.border = javax.swing.BorderFactory.createEmptyBorder(0, 0, 10, 0)
            val skipBreakBtn = JButton("Pomiń przerwę i wracaj do pracy")
            skipBreakBtn.font = skipBreakBtn.font.deriveFont(Font.BOLD, 12f)
            skipBreakBtn.addActionListener { endBreak() }
            bottomPanel.add(skipBreakBtn)

            breakPanel.add(bottomPanel, BorderLayout.SOUTH)
        }

        // --- Logika gry Kółko i Krzyżyk ---

        private fun onPlayerMove(row: Int, col: Int) {
            val btn = tttButtons[row][col]
            if (btn.text.isNotEmpty()) return

            btn.text = "X"
            if (checkWin("X")) {
                Messages.showInfoMessage(toolWindow.project, "Wygrałeś z botem!", "Koniec Gry")
                resetGame()
                return
            }
            if (isBoardFull()) {
                Messages.showInfoMessage(toolWindow.project, "Remis!", "Koniec Gry")
                resetGame()
                return
            }

            botMove()
        }

        private fun botMove() {
            val emptyCells = mutableListOf<Pair<Int, Int>>()
            for (i in 0..2) {
                for (j in 0..2) {
                    if (tttButtons[i][j].text.isEmpty()) {
                        emptyCells.add(Pair(i, j))
                    }
                }
            }

            if (emptyCells.isNotEmpty()) {
                val move = emptyCells.random()
                tttButtons[move.first][move.second].text = "O"

                if (checkWin("O")) {
                    Messages.showInfoMessage(toolWindow.project, "Bot wygrał! Spróbuj jeszcze raz.", "Koniec Gry")
                    resetGame()
                }
            }
        }

        private fun checkWin(player: String): Boolean {
            for (i in 0..2) {
                if (tttButtons[i][0].text == player && tttButtons[i][1].text == player && tttButtons[i][2].text == player) return true
                if (tttButtons[0][i].text == player && tttButtons[1][i].text == player && tttButtons[2][i].text == player) return true
            }
            if (tttButtons[0][0].text == player && tttButtons[1][1].text == player && tttButtons[2][2].text == player) return true
            if (tttButtons[0][2].text == player && tttButtons[1][1].text == player && tttButtons[2][0].text == player) return true
            return false
        }

        private fun isBoardFull(): Boolean {
            return tttButtons.all { row -> row.all { it.text.isNotEmpty() } }
        }

        private fun resetGame() {
            for (i in 0..2) {
                for (j in 0..2) {
                    tttButtons[i][j].text = ""
                }
            }
        }
    }
}