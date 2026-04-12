Excellent — you’re going for that **vibrant red (#FE3131)** as your **primary accent**, which gives your app a strong, warm, social-media vibe (similar to YouTube/Threads-style red).

Here’s a ready-to-use **`THEME_GUIDE.md`** file that documents your Android theme setup — including fonts, icons, color palette, and design philosophy.

---

````{"id":"58124","variant":"standard","title":"Android Theme Guide with #FE3131 Primary Color"}
# 🎨 Android Theme Guide — Modern Rounded UI

## Overview
This theme establishes a **modern, rounded social feed UI** similar to Facebook or Threads.  
It uses **Material 3**, a **soft-rounded design**, and a **vibrant red primary color (#FE3131)** for emphasis and action elements.

---

## 🧱 Theme Foundation

**Base:** `Theme.Material3.Light.NoActionBar`  
**Design Style:** Rounded + Soft Shadows + Minimal Contrast  
**Primary Accent:** `#FE3131`

```xml
<style name="AppTheme" parent="Theme.Material3.Light.NoActionBar">
    <item name="colorPrimary">@color/red_500</item>
    <item name="colorOnPrimary">@color/white</item>
    <item name="colorSurface">@color/white</item>
    <item name="colorSurfaceVariant">@color/off_white</item>
    <item name="colorBackground">@color/off_white</item>
    <item name="android:fontFamily">@font/poppins</item>

    <!-- Rounded global shapes -->
    <item name="shapeAppearanceSmallComponent">@style/RoundedShape.Large</item>
    <item name="shapeAppearanceMediumComponent">@style/RoundedShape.Large</item>
    <item name="shapeAppearanceLargeComponent">@style/RoundedShape.Large</item>

    <!-- Defaults -->
    <item name="buttonStyle">@style/Widget.RoundedButton.Modern</item>
    <item name="editTextStyle">@style/Widget.RoundedInput.Modern</item>
    <item name="cardViewStyle">@style/Widget.RoundedCard.Modern</item>
</style>
```

---

## 🎨 Color Palette (`colors.xml`)
```xml
<resources>
    <color name="red_500">#FE3131</color>
    <color name="off_white">#F9FAFB</color>
    <color name="soft_gray">#EAEAEA</color>
    <color name="black">#000000</color>
    <color name="white">#FFFFFF</color>
</resources>
```

---

## 🔠 Fonts
**Primary Font:** [Poppins](https://fonts.google.com/specimen/Poppins)  
**Secondary Font (Body):** [Inter](https://fonts.google.com/specimen/Inter)

```xml
<item name="android:fontFamily">@font/poppins</item>
```

> Poppins provides a rounded, friendly tone; Inter ensures clear legibility for feeds.

---

## 🧭 Icons

**Primary Library:** [Material Symbols Rounded](https://fonts.google.com/icons?icon.style=Rounded)  
**Supplementary:** [Phosphor Icons](https://phosphoricons.com/) for expressive emojis or reactions.

Install via Gradle:
```gradle
implementation 'com.google.android.material:material:<latest_version>'
```

---

## 🧩 Rounded Component Styles

### Rounded Shape
```xml
<style name="RoundedShape.Large">
    <item name="cornerFamily">rounded</item>
    <item name="cornerSize">20dp</item>
</style>
```

### Rounded Button
```xml
<style name="Widget.RoundedButton.Modern" parent="Widget.Material3.Button">
    <item name="cornerRadius">20dp</item>
    <item name="android:textAllCaps">false</item>
    <item name="android:paddingVertical">12dp</item>
    <item name="android:paddingHorizontal">24dp</item>
    <item name="backgroundTint">@color/red_500</item>
    <item name="android:textColor">@color/white</item>
</style>
```

### Rounded Input Field
```xml
<style name="Widget.RoundedInput.Modern" parent="Widget.Material3.TextInputLayout.OutlinedBox">
    <item name="boxBackgroundMode">outline</item>
    <item name="boxCornerRadiusTopStart">20dp</item>
    <item name="boxCornerRadiusTopEnd">20dp</item>
    <item name="boxCornerRadiusBottomStart">20dp</item>
    <item name="boxCornerRadiusBottomEnd">20dp</item>
    <item name="boxStrokeColor">@color/soft_gray</item>
</style>
```

---

## ✨ Design Notes
- Use **soft shadows** and **elevation (4–6dp)** for depth.
- Keep background colors low-contrast to highlight your red primary.
- Buttons and key actions (Like, Send, Follow) should use the red accent.
- Maintain **8–12dp padding** around all interactive components.
- Avoid harsh borders — rely on shadows and elevation for separation.

---

## 💡 Visual Inspiration
- **Facebook Feed** — rounded cards, clean white surfaces.  
- **Threads / Instagram DM** — subtle shadows, vibrant accents.  
- **Notion Minimalism** — whitespace and calm backgrounds.  

---

**Author:** Blessing  
**Version:** 1.0  
**Theme Codename:** `NeoSocialRed`
````

---

Would you like me to extend this `THEME_GUIDE.md` with **dark mode equivalents** (e.g., softer #FE3131 variant, darker off-white palette)? That makes your theme auto-adapt.
Perfect — now we’re zooming into the **cards**, which are the *core building blocks* of that Facebook-style social look (feed posts, story previews, user suggestions, etc.).

Let’s break this down so your Android XMLs or Compose layouts inherit the same “soft, social feed” vibe automatically.

---

### 🧱 DESIGN GOALS FOR SOCIAL CARDS

1. **Rounded & Floating:** 16–24 dp radius, with subtle elevation.
2. **Soft Shadows:** low opacity (#0000001A–#00000026).
3. **Separated Layers:** each card feels like it’s floating over an off-white surface.
4. **Muted Background:** white surface on a light neutral background (`#F9FAFB`).
5. **Inner Padding:** generous (16–24 dp) to make posts feel airy.
6. **Optional Accent Strip:** a slim red indicator line (`#FE3131`) for active or highlighted cards.

---

### 🧩 XML STYLE DEFINITION

```xml
<resources>

    <!-- Global Rounded Card -->
    <style name="Widget.RoundedCard.Social" parent="Widget.Material3.CardView">
        <item name="cardCornerRadius">24dp</item>
        <item name="cardElevation">6dp</item>
        <item name="cardUseCompatPadding">true</item>
        <item name="cardBackgroundColor">@color/white</item>
        <item name="android:foreground">?android:attr/selectableItemBackground</item>
        <item name="android:padding">20dp</item>
        <item name="android:outlineAmbientShadowColor">#26000000</item>
        <item name="android:outlineSpotShadowColor">#33000000</item>
    </style>

    <!-- Optional: Highlighted Card -->
    <style name="Widget.RoundedCard.Highlighted" parent="@style/Widget.RoundedCard.Social">
        <item name="cardBackgroundColor">@color/off_white</item>
        <item name="android:strokeColor">@color/red_500</item>
        <item name="android:strokeWidth">1dp</item>
    </style>

</resources>
```

Use in layouts:

```xml
<com.google.android.material.card.MaterialCardView
    style="@style/Widget.RoundedCard.Social"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginVertical="8dp">

    <LinearLayout
        android:orientation="vertical"
        android:layout_width="match_parent"
        android:layout_height="wrap_content">

        <!-- Profile Row -->
        <LinearLayout
            android:orientation="horizontal"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:gravity="center_vertical">

            <ImageView
                android:layout_width="40dp"
                android:layout_height="40dp"
                android:src="@drawable/ic_user_avatar"
                android:scaleType="centerCrop"
                android:background="@drawable/avatar_circle_bg" />

            <TextView
                android:text="George Lobko"
                android:textStyle="bold"
                android:textSize="16sp"
                android:layout_marginStart="12dp"
                android:textColor="@color/black" />
        </LinearLayout>

        <!-- Post Content -->
        <TextView
            android:text="Hi everyone, today I was on the most beautiful mountain in the world! 🏔️"
            android:layout_marginTop="12dp"
            android:textColor="@color/black"
            android:textSize="14sp" />

        <!-- Images or media go here -->

        <!-- Action Bar -->
        <LinearLayout
            android:orientation="horizontal"
            android:gravity="center_vertical"
            android:layout_marginTop="12dp">
            <ImageButton
                android:src="@drawable/ic_like_rounded"
                android:tint="@color/gray_600"
                android:background="?attr/selectableItemBackgroundBorderless"
                android:padding="8dp"/>
            <ImageButton
                android:src="@drawable/ic_comment_rounded"
                android:tint="@color/gray_600"
                android:background="?attr/selectableItemBackgroundBorderless"
                android:padding="8dp"/>
        </LinearLayout>
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

---

### 🎨 COLOR + DEPTH TUNING

| Element           | Recommendation                       | Example                                                |
| ----------------- | ------------------------------------ | ------------------------------------------------------ |
| **Card BG**       | White `#FFFFFF`                      | `<item name="cardBackgroundColor">@color/white</item>` |
| **App BG**        | `#F9FAFB`                            | `<item name="colorBackground">@color/off_white</item>` |
| **Shadow**        | 10–15% opacity black                 | `#1A000000`                                            |
| **Accent**        | Primary Red `#FE3131`                | Use for likes, badges, or highlights                   |
| **Corner Radius** | 24dp for main cards, 12dp for nested | —                                                      |

---

### 🧠 ADVANCED TOUCHES

* Add a **tiny red gradient border** around featured posts using `MaterialCardView`’s `strokeColor` with gradient drawable.
* For **reaction icons**, use Phosphor Icons with 20–24 dp size and red/orange/yellow accent tones.
* Use **ripple effects** with low-alpha black for tactile feedback (`?attr/selectableItemBackgroundBorderless`).
* Apply **MotionLayout** or `MaterialFadeThrough` transitions between card states for smoother feed animations.

---

### 💡 TL;DR

**Goal:** Soft, floating cards that breathe.
**Key attributes:** `cardCornerRadius=24dp`, `cardElevation=6dp`, `padding=20dp`, `bg=#FFF`, shadow #0000001A, accent #FE3131.
**Result:** A warm, approachable, “social media feed” design language that matches your theme palette.

---

Would you like me to extend this into a `CARDS_GUIDE.md` companion file (with variants like *Feed Card*, *Story Card*, *Suggestion Card*, etc.*)? It’ll define the consistent system for all card types.
