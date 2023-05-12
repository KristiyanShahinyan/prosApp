package digital.paynetics.phos.screens

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import digital.paynetics.phos.PhosApplication
import digital.paynetics.phos.R
import digital.paynetics.phos.classes.helpers.IntentHelper
import digital.paynetics.phos.classes.lang.PhosString
import digital.paynetics.phos.classes.lang.PhosStringProvider
import digital.paynetics.phos.databinding.FragmentSettingsHelpBinding
import javax.inject.Inject

class SettingsHelpFragment : Fragment() {

    private var _binding: FragmentSettingsHelpBinding? = null

    private val binding get() = _binding!!

    @Inject
    lateinit var stringProvider: PhosStringProvider

    @Inject
    lateinit var intentHelper: IntentHelper

    override fun onAttach(context: Context) {
        PhosApplication.getAppComponent().inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsHelpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initVisibility()
        initTitles()
        initOnClickListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initVisibility() {
        binding.supportCard.visibility =
            if (isSupportEmpty() && isFaqEmpty()) View.GONE else View.VISIBLE

        binding.supportRow.root.visibility = if (isSupportEmpty()) View.GONE else View.VISIBLE
        binding.supportRow.leadingIcon.visibility = View.GONE
        binding.supportRow.leadingIconBackground.visibility = View.GONE

        binding.separator.visibility =
            if (isSupportEmpty() || isFaqEmpty()) View.GONE else View.VISIBLE

        binding.faqRow.root.visibility = if (isFaqEmpty()) View.GONE else View.VISIBLE
        binding.faqRow.leadingIcon.visibility = View.GONE
        binding.faqRow.leadingIconBackground.visibility = View.GONE

        binding.versionRow.leadingIcon.visibility = View.GONE
        binding.versionRow.leadingIconBackground.visibility = View.GONE

        binding.verifyRow.leadingIcon.visibility = View.GONE
        binding.verifyRow.leadingIconBackground.visibility = View.GONE
    }

    private fun initTitles() {
        binding.header.title.text = stringProvider.getString(PhosString.get_help)
        binding.supportRow.title.text = stringProvider.getString(PhosString.contact_support)
        binding.faqRow.title.text = stringProvider.getString(PhosString.faq)
        binding.versionRow.title.text = stringProvider.getString(PhosString.app_version)
        binding.verifyRow.title.text = stringProvider.getString(PhosString.app_verify)
    }

    private fun initOnClickListeners() {
        binding.header.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.supportRow.root.setOnClickListener {
            findNavController().navigate(R.id.action_settings_help_to_support)
        }
        binding.faqRow.root.setOnClickListener {
            intentHelper.getOpenUrlIntent(getString(R.string.faq_url))
                .tryStartActivity(requireContext())
        }
        binding.versionRow.root.setOnClickListener {
            findNavController().navigate(R.id.action_settings_help_to_appVersion)
        }
        binding.verifyRow.root.setOnClickListener {
            findNavController().navigate(R.id.action_settings_help_to_appVerify)
        }
    }

    private fun isSupportEmpty() = getString(R.string.support_phone).isEmpty()
            && getString(R.string.support_email).isEmpty()
            && getString(R.string.support_chat_url).isEmpty()

    private fun isFaqEmpty() = getString(R.string.faq_url).isEmpty()
}