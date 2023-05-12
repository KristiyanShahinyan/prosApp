package digital.paynetics.phos.screens

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import digital.paynetics.phos.PhosApplication
import digital.paynetics.phos.R
import digital.paynetics.phos.classes.enums.BrandingColors
import digital.paynetics.phos.classes.helpers.DoNotDisturbManager
import digital.paynetics.phos.classes.lang.PhosString
import digital.paynetics.phos.classes.lang.PhosStringProvider
import digital.paynetics.phos.common.ImageViewBindings
import digital.paynetics.phos.databinding.FragmentSettingsBinding
import digital.paynetics.phos.sdk.entities.ClientConfig
import javax.inject.Inject

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null

    private val binding get() = _binding!!

    @Inject
    lateinit var stringProvider: PhosStringProvider

    @Inject
    lateinit var dndManager: DoNotDisturbManager

    @Inject
    lateinit var clientConfig: ClientConfig

    override fun onAttach(context: Context) {
        PhosApplication.getAppComponent().inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initVisibility()
        initTitles()
        initOnClickListeners()
        initIcons()
    }

    override fun onStart() {
        super.onStart()
        updateDndState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initVisibility() {
        binding.header.title.visibility = View.GONE
        binding.contactName.visibility =
            if (clientConfig.merchant.name?.isBlank() == true) View.GONE else View.VISIBLE
        binding.contactEmail.visibility =
            if (clientConfig.merchant.email?.isBlank() == true) View.GONE else View.VISIBLE
        binding.logoutRow.trailingIcon.visibility = View.GONE
    }

    private fun initTitles() {
        binding.header.title.text = stringProvider.getString(PhosString.settings)
        binding.contactName.text = clientConfig.merchant.name
        binding.contactEmail.text = clientConfig.merchant.email
        binding.merchantName.text = clientConfig.terminal.merchantName
        binding.merchantId.text = clientConfig.terminal.merchantId
        binding.terminalId.text = clientConfig.terminal.terminalId
        binding.terminalCurrency.text = clientConfig.terminal.currency
        binding.titleSettings.text = stringProvider.getString(PhosString.settings)
        binding.dndRow.title.text = stringProvider.getString(PhosString.dnd_title)
        binding.dndRow.hint.text = stringProvider.getString(PhosString.dnd_hint)
        binding.passwordRow.title.text = stringProvider.getString(PhosString.change_password)
        binding.languageRow.title.text = stringProvider.getString(PhosString.switch_language)
        binding.printersRow.title.text = stringProvider.getString(PhosString.set_printer)
        binding.helpRow.title.text = stringProvider.getString(PhosString.get_help)
        binding.screensRow.title.text = stringProvider.getString(PhosString.screen_preferences)
        binding.logoutRow.title.text = stringProvider.getString(PhosString.log_out)
    }

    private fun initOnClickListeners() {
        binding.header.btnBack.setOnClickListener { requireActivity().finish() }
        binding.dndRow.toggle.setOnClickListener {
            dndManager.toggle(requireActivity())
            updateDndState()
        }
        binding.passwordRow.root.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_changePass)
        }
        binding.languageRow.root.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_language)
        }
        binding.printersRow.root.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_printers)
        }
        binding.helpRow.root.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_help)
        }
        binding.screensRow.root.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_screens)
        }
        binding.logoutCard.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_logout)
        }
    }

    private fun initIcons() {
        setIcon(binding.dndRow.leadingIcon, R.drawable.ic_dnd_16dp)
        setIcon(binding.passwordRow.leadingIcon, R.drawable.ic_change_pass_16dp)
        setIcon(binding.languageRow.leadingIcon, R.drawable.ic_change_language_16dp)
        setIcon(binding.printersRow.leadingIcon, R.drawable.ic_printer_16dp)
        setIcon(binding.helpRow.leadingIcon, R.drawable.ic_help_16dp)
        setIcon(binding.screensRow.leadingIcon, R.drawable.ic_prefs_16dp)
        setIcon(binding.logoutRow.leadingIcon, R.drawable.ic_logout_16dp)
    }

    private fun updateDndState() {
        if (!dndManager.isSupported) {
            return
        }
        dndManager.resetOnNoAccessGranted()
        binding.dndRow.toggle.isChecked = dndManager.isAccessGrantedAndEnabled
    }

    private fun setIcon(view: ImageView, @DrawableRes icon: Int) {
        view.setImageDrawable(tint(icon))
    }

    private fun tint(@DrawableRes drawableRes: Int) = ImageViewBindings.tintDrawable(
        ImageViewBindings.getDrawable(requireActivity(), drawableRes),
        clientConfig.getDynamicColor(BrandingColors.PRIMARY_COLOR)
    )

    companion object {
        private const val TAG = "SettingsFragment"
    }
}