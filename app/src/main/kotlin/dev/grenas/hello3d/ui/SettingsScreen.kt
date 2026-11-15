package dev.grenas.hello3d.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import dev.grenas.hello3d.data.Languages
import dev.grenas.hello3d.data.Profile
import dev.grenas.hello3d.data.RainIntensity
import dev.grenas.hello3d.data.RenderPreset
import dev.grenas.hello3d.ui.components.Glass
import dev.grenas.hello3d.ui.components.SectionLabel
import dev.grenas.hello3d.ui.theme.ThemePack
import dev.grenas.hello3d.ui.theme.ThemePacks

/** Everything that shapes the sentence and the look: identity, languages, theme pack and render options. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    profile: Profile,
    pack: ThemePack,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onToggleLanguage: (String) -> Unit,
    onLanguagesChange: (List<String>) -> Unit,
    onThemeSelected: (String) -> Unit,
    onQualitySelected: (RenderPreset) -> Unit,
    onRainSelected: (RainIntensity) -> Unit,
) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = pack.textPrimary,
                    navigationIconContentColor = pack.textPrimary,
                ),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(pack.backdrop))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                IdentitySection(profile, pack, onNameChange, onLocationChange)
                LanguagesSection(profile, pack, onToggleLanguage, onLanguagesChange)
                ThemeSection(selectedId = profile.themeId, onThemeSelected = onThemeSelected)
                RenderSection(profile, pack, onQualitySelected, onRainSelected)
            }
        }
    }
}

// ── Sections ──────────────────────────────────────────────────────────────────────────────────────

@Composable
private fun Section(title: String, description: String? = null, content: @Composable () -> Unit) {
    Glass(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            SectionLabel(title)
            if (description != null) {
                Spacer(Modifier.height(4.dp))
                Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun IdentitySection(
    profile: Profile,
    pack: ThemePack,
    onNameChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
) {
    // Local copies keep typing responsive; every change is persisted immediately.
    var name by rememberSaveable { mutableStateOf(profile.name) }
    var location by rememberSaveable { mutableStateOf(profile.location) }
    LaunchedEffect(profile.name) { if (profile.name != name) name = profile.name }
    LaunchedEffect(profile.location) { if (profile.location != location) location = profile.location }

    Section(title = "IDENTITY", description = "Typed into the scene as \"my name is …\" and \"based in …\".") {
        ThemedTextField(
            value = name,
            onValueChange = { v -> name = v.take(Profile.MAX_NAME); onNameChange(name) },
            label = "Name",
            pack = pack,
            supporting = "${name.length} / ${Profile.MAX_NAME}",
        )
        Spacer(Modifier.height(10.dp))
        ThemedTextField(
            value = location,
            onValueChange = { v -> location = v.take(Profile.MAX_LOCATION); onLocationChange(location) },
            label = "Location",
            pack = pack,
            supporting = "${location.length} / ${Profile.MAX_LOCATION}",
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun LanguagesSection(
    profile: Profile,
    pack: ThemePack,
    onToggleLanguage: (String) -> Unit,
    onLanguagesChange: (List<String>) -> Unit,
) {
    var custom by rememberSaveable { mutableStateOf("") }
    val full = profile.languages.size >= Profile.MAX_LANGUAGES
    fun addCustom() {
        val value = Languages.normalise(custom)
        if (value.isNotEmpty() && !full && value !in profile.languages) {
            onLanguagesChange(profile.languages + value)
        }
        custom = ""
    }

    Section(
        title = "LANGUAGES",
        description = "Cycled through with a matrix scramble. Up to ${Profile.MAX_LANGUAGES}, in the order you pick them.",
    ) {
        if (profile.languages.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                profile.languages.forEach { language ->
                    InputChip(
                        selected = true,
                        onClick = { onLanguagesChange(profile.languages - language) },
                        label = { Text(language) },
                        trailingIcon = { Icon(Icons.Filled.Close, contentDescription = "Remove $language", modifier = Modifier.size(16.dp)) },
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            ThemedTextField(
                value = custom,
                onValueChange = { custom = it.take(Profile.MAX_LANGUAGE_LENGTH) },
                label = if (full) "List is full" else "Add your own",
                pack = pack,
                enabled = !full,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { addCustom() }),
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { addCustom() }, enabled = !full && custom.isNotBlank()) {
                Icon(Icons.Filled.Add, contentDescription = "Add language", tint = pack.accent)
            }
        }

        Spacer(Modifier.height(14.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Languages.curated.forEach { language ->
                val selected = language in profile.languages
                FilterChip(
                    selected = selected,
                    enabled = selected || !full,
                    onClick = { onToggleLanguage(language) },
                    label = { Text(language) },
                    leadingIcon = if (selected) {
                        { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = pack.accent.copy(alpha = 0.22f),
                        selectedLabelColor = pack.textPrimary,
                        selectedLeadingIconColor = pack.accent,
                        labelColor = pack.textMuted,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = selected || !full,
                        selected = selected,
                        borderColor = pack.textPrimary.copy(alpha = 0.14f),
                        selectedBorderColor = pack.accent.copy(alpha = 0.6f),
                    ),
                )
            }
        }
    }
}

@Composable
private fun ThemeSection(selectedId: String, onThemeSelected: (String) -> Unit) {
    Section(title = "THEME", description = "Colours, metal finish, typeface and the glyphs that rain.") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ThemePacks.all.forEach { pack ->
                ThemeCard(pack = pack, selected = pack.id == selectedId) { onThemeSelected(pack.id) }
            }
        }
    }
}

@Composable
private fun ThemeCard(pack: ThemePack, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.horizontalGradient(pack.backdrop.reversed()))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) pack.accent else Color.White.copy(alpha = 0.12f),
                shape = shape,
            )
            .clickable(onClick = onClick)
            .semantics { contentDescription = "${pack.name} theme${if (selected) ", selected" else ""}" }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(pack.accent, pack.accentAlt)))
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(pack.name, style = MaterialTheme.typography.titleMedium, color = pack.textPrimary)
            Text(pack.tagline, style = MaterialTheme.typography.bodyMedium, color = pack.textMuted)
        }
        if (selected) {
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Filled.Check, contentDescription = null, tint = pack.accent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RenderSection(
    profile: Profile,
    pack: ThemePack,
    onQualitySelected: (RenderPreset) -> Unit,
    onRainSelected: (RainIntensity) -> Unit,
) {
    Section(title = "RENDER", description = "Filament quality preset and matrix-rain density.") {
        SectionLabel("QUALITY")
        Spacer(Modifier.height(8.dp))
        Segmented(
            options = RenderPreset.entries,
            selected = profile.quality,
            label = { it.name },
            pack = pack,
            onSelected = onQualitySelected,
        )
        Spacer(Modifier.height(14.dp))
        SectionLabel("MATRIX RAIN")
        Spacer(Modifier.height(8.dp))
        Segmented(
            options = RainIntensity.entries,
            selected = profile.rainIntensity,
            label = { it.name },
            pack = pack,
            onSelected = onRainSelected,
        )
    }
}

// ── Themed controls ───────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> Segmented(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    pack: ThemePack,
    onSelected: (T) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelected(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = pack.accent.copy(alpha = 0.18f),
                    activeContentColor = pack.textPrimary,
                    activeBorderColor = pack.accent.copy(alpha = 0.6f),
                    inactiveContainerColor = Color.Transparent,
                    inactiveContentColor = pack.textMuted,
                    inactiveBorderColor = pack.textPrimary.copy(alpha = 0.14f),
                ),
                icon = {},
            ) {
                Text(label(option), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun ThemedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    pack: ThemePack,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    supporting: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        supportingText = supporting?.let { { Text(it) } },
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        modifier = modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = pack.accent,
            unfocusedBorderColor = pack.textPrimary.copy(alpha = 0.2f),
            focusedLabelColor = pack.accent,
            unfocusedLabelColor = pack.textMuted,
            cursorColor = pack.accent,
            focusedTextColor = pack.textPrimary,
            unfocusedTextColor = pack.textPrimary,
            focusedSupportingTextColor = pack.textMuted,
            unfocusedSupportingTextColor = pack.textMuted,
        ),
    )
}
