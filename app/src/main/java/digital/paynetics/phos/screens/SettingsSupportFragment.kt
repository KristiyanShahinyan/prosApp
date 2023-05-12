package digital.paynetics.phos.screens

import android.content.Context
import android.content.Intent
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
import digital.paynetics.phos.databinding.FragmentSettingsSupportBinding
import javax.inject.Inject

class SettingsSupportFragment : Fragment(/*R.layout.fragment_settings_support*/) {

    private var _binding: FragmentSettingsSupportBinding? = null

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
        _binding = FragmentSettingsSupportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (getString(R.string.support_phone).isEmpty()) {
            binding.callCard.root.visibility = View.GONE
        }
        if (getString(R.string.support_email).isEmpty()) {
            binding.emailCard.root.visibility = View.GONE
        }
        if (getString(R.string.support_chat_url).isEmpty()) {
            binding.chatCard.root.visibility = View.GONE
        }

        binding.header.title.text = stringProvider.getString(PhosString.contact_support)
        binding.callCard.title.text = stringProvider.getString(PhosString.support_call)
        binding.emailCard.title.text = stringProvider.getString(PhosString.support_email)
        binding.chatCard.title.text = stringProvider.getString(PhosString.support_chat)

        binding.callCard.value.text = getString(R.string.support_phone)
        binding.emailCard.value.text = getString(R.string.support_email)
        binding.chatCard.value.text = getString(R.string.support_chat_url)

        binding.header.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.callCard.root.setOnClickListener { openDialerActivity() }
        binding.emailCard.root.setOnClickListener { openEmailActivity() }
        binding.chatCard.root.setOnClickListener { openBrowserActivity() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun openDialerActivity() {
        val intent = intentHelper.getDialPhoneIntent(getString(R.string.support_phone))
        tryStartActivity(intent)
    }

    private fun openEmailActivity() {
        val intent = intentHelper.getSendEmailIntent(getString(R.string.support_email))
        tryStartActivity(intent)
    }

    private fun openBrowserActivity() {
        val intent = intentHelper.getOpenUrlIntent(getString(R.string.support_chat_url))
        tryStartActivity(intent)
    }

    private fun tryStartActivity(intent: Intent) {
        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(intent)
        }
    }
}