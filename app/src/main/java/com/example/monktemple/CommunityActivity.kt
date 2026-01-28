package com.example.monktemple

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.monktemple.community.FocusCommunityManager
import com.example.monktemple.databinding.ActivityCommunityBinding
import com.example.monktemple.Utlis.SessionManager
import com.example.monktemple.Utlis.ShowToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CommunityActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCommunityBinding

    @Inject
    lateinit var communityManager: FocusCommunityManager

    @Inject
    lateinit var sessionManager: SessionManager

    private lateinit var backButton: Button
    private lateinit var createCommunityButton: Button
    private lateinit var joinCommunityButton: Button
    private lateinit var insightsButton: Button
    private lateinit var securityButton: Button
    private lateinit var customizationButton: Button
    private lateinit var communitiesRecyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var communitiesAdapter: CommunitiesAdapter

    private var communities = listOf<FocusCommunityManager.FocusCommunity>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCommunityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initializeViews()
        setupRecyclerView()
        setupClickListeners()
        loadCommunities()
    }

    private fun initializeViews() {
        backButton = binding.backButton
        createCommunityButton = binding.createCommunityButton
        joinCommunityButton = binding.joinCommunityButton
        insightsButton = binding.insightsButton
        securityButton = binding.securityButton
        customizationButton = binding.customizationButton
        communitiesRecyclerView = binding.communitiesRecyclerView
        progressBar = binding.progressBar
    }

    private fun setupRecyclerView() {
        communitiesAdapter = CommunitiesAdapter(communities) { community ->
            showCommunityDetails(community)
        }

        communitiesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@CommunityActivity)
            adapter = communitiesAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            finish()
        }

        createCommunityButton.setOnClickListener {
            showCreateCommunityDialog()
        }

        joinCommunityButton.setOnClickListener {
            showJoinCommunityDialog()
        }

        insightsButton.setOnClickListener {
            startActivity(Intent(this, InsightsActivity::class.java))
        }

        securityButton.setOnClickListener {
            startActivity(Intent(this, SecurityActivity::class.java))
        }

        customizationButton.setOnClickListener {
            startActivity(Intent(this, CustomizationActivity::class.java))
        }
    }

    private fun loadCommunities() {
        lifecycleScope.launch {
            try {
                progressBar.visibility = android.view.View.VISIBLE

                val userId = sessionManager.getFirebaseUid()
                if (userId.isNullOrEmpty()) {
                    ShowToast("Please log in to view communities")
                    return@launch
                }

                // Load user's communities
                val userCommunitiesResult = communityManager.getUserCommunities(userId)
                if (userCommunitiesResult.isSuccess) {
                    communities = userCommunitiesResult.getOrNull() ?: emptyList()
                    communitiesAdapter.updateCommunities(communities)
                } else {
                    ShowToast("Error loading communities")
                }

                // Load public communities for discovery
                loadPublicCommunities()

            } catch (e: Exception) {
                ShowToast("Error loading communities")
                e.printStackTrace()
            } finally {
                progressBar.visibility = android.view.View.GONE
            }
        }
    }

    private fun loadPublicCommunities() {
        lifecycleScope.launch {
            try {
                val publicCommunitiesResult = communityManager.getPublicCommunities()
                if (publicCommunitiesResult.isSuccess) {
                    val publicCommunities = publicCommunitiesResult.getOrNull() ?: emptyList()
                    // You could show these in a separate section
                    if (communities.isEmpty() && publicCommunities.isNotEmpty()) {
                        binding.discoveryText.visibility = android.view.View.VISIBLE
                        binding.discoveryText.text = "Discover ${publicCommunities.size} public communities"
                    }
                }
            } catch (e: Exception) {
                // Silent fail for public communities
            }
        }
    }

    private fun showCreateCommunityDialog() {
        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("Create Focus Community")
            .setMessage("Create a community to focus together with friends or like-minded people.")
            .setPositiveButton("Create") { dialog, _ ->
                createNewCommunity()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .create()

        dialog.show()
    }

    private fun showJoinCommunityDialog() {
        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("Join Community")
            .setMessage("Enter community ID or browse public communities.")
            .setPositiveButton("Browse") { dialog, _ ->
                showPublicCommunities()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .setNeutralButton("Enter ID") { dialog, _ ->
                showEnterCommunityIdDialog()
                dialog.dismiss()
            }
            .create()

        dialog.show()
    }

    private fun showEnterCommunityIdDialog() {
        val input = android.widget.EditText(this)
        input.hint = "Enter Community ID"

        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("Join Community by ID")
            .setView(input)
            .setPositiveButton("Join") { dialog, _ ->
                val communityId = input.text.toString().trim()
                if (communityId.isNotEmpty()) {
                    joinCommunityById(communityId)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .create()

        dialog.show()
    }

    private fun createNewCommunity() {
        lifecycleScope.launch {
            try {
                val userId = sessionManager.getFirebaseUid() ?: return@launch

                val newCommunity = FocusCommunityManager.FocusCommunity(
                    name = "My Focus Group",
                    description = "Let's focus together and achieve our goals!",
                    focusGoal = "Productivity & Mindfulness",
                    createdBy = userId,
                    isPublic = false,
                    tags = listOf("focus", "productivity", "mindfulness")
                )

                val result = communityManager.createCommunity(newCommunity)
                if (result.isSuccess) {
                    ShowToast("Community created successfully!")
                    loadCommunities() // Refresh list
                } else {
                    ShowToast("Error creating community")
                }
            } catch (e: Exception) {
                ShowToast("Error creating community")
            }
        }
    }

    private fun joinCommunityById(communityId: String) {
        lifecycleScope.launch {
            try {
                val userId = sessionManager.getFirebaseUid() ?: return@launch

                val result = communityManager.joinCommunity(communityId, userId)
                if (result.isSuccess) {
                    ShowToast("Joined community successfully!")
                    loadCommunities() // Refresh list
                } else {
                    ShowToast("Error joining community")
                }
            } catch (e: Exception) {
                ShowToast("Error joining community")
            }
        }
    }

    private fun showCommunityDetails(community: FocusCommunityManager.FocusCommunity) {
        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle(community.name)
            .setMessage("""
                ${community.description}
                
                👥 Members: ${community.memberCount}
                🎯 Focus: ${community.focusGoal}
                📍 ${if (community.isPublic) "Public" else "Private"}
            """.trimIndent())
            .setPositiveButton("Open") { dialog, _ ->
                openCommunity(community)
                dialog.dismiss()
            }
            .setNegativeButton("Close") { dialog, _ -> dialog.dismiss() }
            .create()

        dialog.show()
    }

    private fun openCommunity(community: FocusCommunityManager.FocusCommunity) {
        // Navigate to community detail activity or start group session
        ShowToast("Opening ${community.name}")
        // You would implement community detail view here
    }

    private fun showPublicCommunities() {
        // Implement public communities browser
        ShowToast("Public communities feature coming soon!")
    }

    override fun onResume() {
        super.onResume()
        loadCommunities()
    }
}

// Simple RecyclerView Adapter
class CommunitiesAdapter(
    private var communities: List<FocusCommunityManager.FocusCommunity>,
    private val onItemClick: (FocusCommunityManager.FocusCommunity) -> Unit
) : RecyclerView.Adapter<CommunitiesAdapter.CommunityViewHolder>() {

    class CommunityViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.communityName)
        val description: TextView = itemView.findViewById(R.id.communityDescription)
        val memberCount: TextView = itemView.findViewById(R.id.memberCount)
        val focusGoal: TextView = itemView.findViewById(R.id.focusGoal)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): CommunityViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_community, parent, false)
        return CommunityViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommunityViewHolder, position: Int) {
        val community = communities[position]

        holder.name.text = community.name
        holder.description.text = community.description
        holder.memberCount.text = "${community.memberCount} members"
        holder.focusGoal.text = community.focusGoal

        holder.itemView.setOnClickListener {
            onItemClick(community)
        }
    }

    override fun getItemCount(): Int = communities.size

    fun updateCommunities(newCommunities: List<FocusCommunityManager.FocusCommunity>) {
        communities = newCommunities
        notifyDataSetChanged()
    }
}
