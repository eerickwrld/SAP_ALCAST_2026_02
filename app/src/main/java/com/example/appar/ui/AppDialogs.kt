package com.example.appar.ui

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.example.appar.data.PreferencesRepository

class AppDialogs(private val context: Context) {

    interface MenuCallback {
        fun onReload()
        fun onChangeScale()
        fun onReturnToStart()
        fun onCheckUpdates()
        fun onShowAbout()
        fun onLogoffRequested()
    }

    fun showMainMenu(callback: MenuCallback) {
        val options = arrayOf(
            "Recarregar SAP",
            "Tamanho dos textos",
            "Voltar ao início",
            "Verificar Atualizações",
            "Sobre o Aplicativo",
            "Logoff"
        )
        AlertDialog.Builder(context)
            .setTitle("SAP ALCAST • Menu")
            .setItems(options) { _, index ->
                when (index) {
                    0 -> callback.onReload()
                    1 -> callback.onChangeScale()
                    2 -> callback.onReturnToStart()
                    3 -> callback.onCheckUpdates()
                    4 -> callback.onShowAbout()
                    5 -> callback.onLogoffRequested()
                }
            }
            .show()
    }

    fun showScaleChooser(
        currentScale: Int,
        onScaleSelected: (newScale: Int) -> Unit
    ) {
        val scales = PreferencesRepository.AVAILABLE_SCALES
        val options = PreferencesRepository.SCALE_LABELS
        val selectedIndex = scales.indexOf(currentScale).coerceAtLeast(0)

        AlertDialog.Builder(context)
            .setTitle("Tamanho dos textos")
            .setSingleChoiceItems(options, selectedIndex) { dialog, selected ->
                onScaleSelected(scales[selected])
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    fun showReturnToStartConfirmation(onConfirm: () -> Unit) {
        AlertDialog.Builder(context)
            .setTitle("Voltar ao início?")
            .setMessage("A operação atual pode ser interrompida.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Continuar") { _, _ -> onConfirm() }
            .show()
    }

    fun showAboutDialog() {
        AlertDialog.Builder(context)
            .setTitle("SAP ALCAST")
            .setMessage("Interface simplificada para coletores Android com SAP ITS Mobile. A leitura física permanece sob controle do coletor. " +
                    "Desenvolvido por Erick Jaboinski e Marcos Gustavo de Lima. Copyright © 2026 Erick Jaboinski. Todos os direitos reservados.")
            .setPositiveButton("OK", null)
            .show()
    }

    fun showLogoffConfirmation(onConfirm: () -> Unit) {
        AlertDialog.Builder(context)
            .setTitle("Sair do SAP?")
            .setMessage("A sessão deste coletor será limpa e a tela de entrada do SAP será aberta. Operações não salvas podem ser perdidas.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Logoff") { _, _ -> onConfirm() }
            .show()
    }
}
