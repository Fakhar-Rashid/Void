package com.example.avoid.dev;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.avoid.model.Color;
import com.example.avoid.model.Condition;
import com.example.avoid.model.Product;
import com.example.avoid.model.ProductCategory;
import com.example.avoid.model.WeightUnit;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * One-shot dev seeder. Writes a curated catalog of laptops / smartphones / monitors /
 * accessories across the three demo stores, and patches each of those stores with a logo +
 * banner URL so the store details page renders nicely.
 *
 * <p>Images are public Unsplash CDN URLs picked to match each product family (not the exact
 * SKU). For prod use, swap them out via the seller dashboard.
 *
 * <p>Trigger via {@link #seedAll(Callback)} — wired behind a long-press on the buyer home
 * title in {@code HomeFragment}. Safe to call multiple times — products are added new each
 * run; store docs are merged so duplicate runs simply overwrite the logo/banner.
 */
public final class ProductSeeder {

    private static final String TAG = "ProductSeeder";
    private static final String COLLECTION_PRODUCTS = "products";
    private static final String COLLECTION_STORES   = "stores";

    // Three demo stores (matching the docs you shared).
    public static final String STORE_RAID_ID    = "1apNADkXdZSIdpRzX0GZufO6ad23";
    public static final String STORE_SAIF_ID    = "Gbgloq97icemko8Aa5vOTvRMaNF2";
    public static final String STORE_ORGANO_ID  = "Mqsp93r3BZZPniN3QtOcgIlK7152";

    private static final String STORE_RAID_NAME   = "Raid";
    private static final String STORE_SAIF_NAME   = "Saifullah Store";
    private static final String STORE_ORGANO_NAME = "Organo";
    private static final String LOCATION = "Lahore";

    public interface Callback {
        void onDone(int productsAdded, int storesPatched);
        void onError(@NonNull Exception e);
    }

    private ProductSeeder() {}

    /**
     * Re-patches the three demo stores with tech-themed logo + banner URLs. Use this when
     * the original {@link #seedAll} banners didn't fit (the first pass picked clothing-store
     * shots by mistake).
     */
    public static void seedStoreBranding(@NonNull Callback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        List<Task<Void>> writes = new ArrayList<>();

        Map<String, Object> raidPatch = storePatch(
                // Tech logo / device close-up.
                "https://images.unsplash.com/photo-1593642632559-0c6d3fc62b89?w=400&q=80&auto=format&fit=crop",
                // Wide tech flat-lay banner.
                "https://images.unsplash.com/photo-1498049794561-7780e7231661?w=1600&q=80&auto=format&fit=crop");

        Map<String, Object> saifPatch = storePatch(
                // Smartphone close-up logo.
                "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=400&q=80&auto=format&fit=crop",
                // Mobile-shop banner — phones lined up.
                "https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?w=1600&q=80&auto=format&fit=crop");

        Map<String, Object> organoPatch = storePatch(
                // Premium minimal device.
                "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=400&q=80&auto=format&fit=crop",
                // Premium tech workspace banner.
                "https://images.unsplash.com/photo-1550009158-9ebf69173e03?w=1600&q=80&auto=format&fit=crop");

        writes.add(db.collection(COLLECTION_STORES).document(STORE_RAID_ID).set(raidPatch,
                com.google.firebase.firestore.SetOptions.merge()));
        writes.add(db.collection(COLLECTION_STORES).document(STORE_SAIF_ID).set(saifPatch,
                com.google.firebase.firestore.SetOptions.merge()));
        writes.add(db.collection(COLLECTION_STORES).document(STORE_ORGANO_ID).set(organoPatch,
                com.google.firebase.firestore.SetOptions.merge()));

        Tasks.whenAllComplete(writes).addOnCompleteListener(task -> {
            int failed = 0;
            for (Task<?> t : task.getResult()) if (!t.isSuccessful()) failed++;
            if (failed > 0) {
                Exception e = task.getResult().get(0).getException();
                Log.e(TAG, failed + " store branding writes failed", e);
                callback.onError(e != null ? e : new RuntimeException(failed + " writes failed"));
                return;
            }
            callback.onDone(0, 3);
        });
    }

    /**
     * Adds 12 fresh products (4 per store) — completely distinct SKUs from {@link #seedAll}.
     * Skips the store-doc patches (assumes those already ran). Safe to run alongside, or
     * after, the initial seeder.
     */
    public static void seedExtras(@NonNull Callback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        List<Product> extras = buildExtraCatalog();
        List<Task<Void>> writes = new ArrayList<>();
        for (Product p : extras) {
            writes.add(db.collection(COLLECTION_PRODUCTS).document().set(p));
        }
        Tasks.whenAllComplete(writes).addOnCompleteListener(task -> {
            int failed = 0;
            for (Task<?> t : task.getResult()) if (!t.isSuccessful()) failed++;
            if (failed > 0) {
                Exception e = task.getResult().get(0).getException();
                Log.e(TAG, failed + " writes failed", e);
                callback.onError(e != null ? e : new RuntimeException(failed + " writes failed"));
                return;
            }
            callback.onDone(extras.size(), 0);
        });
    }

    public static void seedAll(@NonNull Callback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        List<Task<Void>> writes = new ArrayList<>();

        // 1) Patch the three store docs with logo + banner so the store page looks right.
        Map<String, Object> raidPatch = storePatch(
                "https://images.unsplash.com/photo-1556742502-ec7c0e9f34b1?w=400&q=80&auto=format&fit=crop",
                "https://images.unsplash.com/photo-1441986300917-64674bd600d8?w=1200&q=80&auto=format&fit=crop");
        Map<String, Object> saifPatch = storePatch(
                "https://images.unsplash.com/photo-1607082348824-0a96f2a4b9da?w=400&q=80&auto=format&fit=crop",
                "https://images.unsplash.com/photo-1556761175-5973dc0f32e7?w=1200&q=80&auto=format&fit=crop");
        Map<String, Object> organoPatch = storePatch(
                "https://images.unsplash.com/photo-1542838132-92c53300491e?w=400&q=80&auto=format&fit=crop",
                "https://images.unsplash.com/photo-1483985988355-763728e1935b?w=1200&q=80&auto=format&fit=crop");

        writes.add(db.collection(COLLECTION_STORES).document(STORE_RAID_ID).set(raidPatch,
                com.google.firebase.firestore.SetOptions.merge()));
        writes.add(db.collection(COLLECTION_STORES).document(STORE_SAIF_ID).set(saifPatch,
                com.google.firebase.firestore.SetOptions.merge()));
        writes.add(db.collection(COLLECTION_STORES).document(STORE_ORGANO_ID).set(organoPatch,
                com.google.firebase.firestore.SetOptions.merge()));

        // 2) Build the catalog and add() each product.
        List<Product> catalog = buildCatalog();
        for (Product p : catalog) {
            DocumentReference ref = db.collection(COLLECTION_PRODUCTS).document();
            // doing set() so DocumentId is preserved on subsequent reads via @DocumentId.
            writes.add(ref.set(p));
        }

        Tasks.whenAllComplete(writes).addOnCompleteListener(task -> {
            int failed = 0;
            for (Task<?> t : task.getResult()) if (!t.isSuccessful()) failed++;
            if (failed > 0) {
                Exception e = task.getResult().get(0).getException();
                Log.e(TAG, failed + " writes failed", e);
                callback.onError(e != null ? e : new RuntimeException(failed + " writes failed"));
                return;
            }
            callback.onDone(catalog.size(), 3);
        });
    }

    private static Map<String, Object> storePatch(String logoUrl, String bannerUrl) {
        Map<String, Object> m = new HashMap<>();
        m.put("logoUrl", logoUrl);
        m.put("bannerUrl", bannerUrl);
        return m;
    }

    // ──────────────────────────── Catalog ────────────────────────────

    private static List<Product> buildCatalog() {
        long now = System.currentTimeMillis();
        List<Product> products = new ArrayList<>();
        int i = 0;

        // ── Raid: general tech retailer (6 items) ─────────────────────
        products.add(p(now - (i++) * 60_000L,
                "MacBook Pro 14\" M3 Pro",
                "Apple's MacBook Pro 14\" with the M3 Pro chip is built for serious creative work. " +
                        "An 18-core GPU, 18 GB unified memory, and a 1 TB SSD breeze through 8K editing, " +
                        "Xcode builds, and heavy Lightroom catalogs. The mini-LED Liquid Retina XDR display " +
                        "hits 1,000 nits sustained brightness with ProMotion, and the chassis stays cool and " +
                        "quiet under load. Includes the 96W USB-C charger and a year of AppleCare.",
                1599.00, ProductCategory.LAPTOP, Condition.NEW, 1.6, WeightUnit.KILOGRAMS,
                STORE_RAID_ID, STORE_RAID_NAME, 18, 4.8, 142,
                colors("Black", "Silver"),
                imgs(LAPTOP_1, LAPTOP_2, LAPTOP_3)));

        products.add(p(now - (i++) * 60_000L,
                "iPhone 15 Pro 256 GB",
                "The iPhone 15 Pro brings the A17 Pro chip, a titanium frame, and the new Action button " +
                        "to a 6.1\" Super Retina XDR display. Triple-camera system with a 48 MP main, 12 MP " +
                        "ultrawide and a 3× telephoto handles low-light and action shots cleanly. " +
                        "USB-C with USB 3 speeds, Wi-Fi 6E, and all-day battery life. Sealed retail box, " +
                        "Apple warranty intact, charging cable included.",
                999.00, ProductCategory.SMARTPHONE, Condition.NEW, 187, WeightUnit.GRAMS,
                STORE_RAID_ID, STORE_RAID_NAME, 24, 4.7, 312,
                colors("Black", "White", "Silver"),
                imgs(PHONE_1, PHONE_2)));

        products.add(p(now - (i++) * 60_000L,
                "Sony WH-1000XM5 Wireless Headphones",
                "Sony's flagship over-ear with industry-leading active noise cancellation. New 30 mm " +
                        "carbon-fibre drivers, eight microphones for clearer calls, and 30-hour battery " +
                        "with quick-charge (3 minutes for 3 hours). Soft synthetic leather earcups, " +
                        "lightweight at 250 g, and Multipoint pairs to two devices at once. Comes with " +
                        "the carry case, USB-C cable and 3.5 mm aux cable.",
                399.00, ProductCategory.ACCESSORIES, Condition.NEW, 250, WeightUnit.GRAMS,
                STORE_RAID_ID, STORE_RAID_NAME, 30, 4.9, 488,
                colors("Black", "Silver"),
                imgs(HEADPHONES_1, HEADPHONES_2)));

        products.add(p(now - (i++) * 60_000L,
                "Dell UltraSharp 27\" 4K USB-C Monitor",
                "27-inch 4K (3840×2160) IPS panel calibrated to 99% sRGB and DeltaE < 2 out of the box. " +
                        "Single USB-C cable delivers 90W power and DisplayPort video to a connected laptop, " +
                        "with a 4-port USB hub on the back. InfinityEdge bezel, fully ergonomic stand " +
                        "(tilt / swivel / pivot / height), and a 3-year Premium Panel Exchange warranty.",
                649.00, ProductCategory.MONITOR, Condition.NEW, 6.1, WeightUnit.KILOGRAMS,
                STORE_RAID_ID, STORE_RAID_NAME, 12, 4.6, 88,
                colors("Black", "Silver"),
                imgs(MONITOR_1, MONITOR_2)));

        products.add(p(now - (i++) * 60_000L,
                "Samsung Galaxy S24 Ultra 512 GB",
                "Galaxy S24 Ultra with the Snapdragon 8 Gen 3 for Galaxy, a 6.8\" QHD+ Dynamic AMOLED " +
                        "2X with 120 Hz, and a built-in S Pen. Quad-camera with 200 MP main, 10 MP 3× and " +
                        "50 MP 5× telephotos, plus on-device Galaxy AI for live translation and circle-to-search. " +
                        "Titanium frame, IP68 water resistance, 5,000 mAh battery with 45 W wired charging.",
                1299.00, ProductCategory.SMARTPHONE, Condition.NEW, 232, WeightUnit.GRAMS,
                STORE_RAID_ID, STORE_RAID_NAME, 18, 4.8, 224,
                colors("Black", "Gold", "Silver"),
                imgs(PHONE_3, PHONE_4)));

        products.add(p(now - (i++) * 60_000L,
                "Logitech MX Master 3S Mouse",
                "The MX Master 3S keeps the MagSpeed scroll wheel (1,000 lines per second!) and adds " +
                        "an 8K-DPI sensor that tracks on glass. New Quiet Clicks reduce click noise by " +
                        "90%. USB-C charging gives ~70 days per charge, and Logi Options+ lets you " +
                        "customise per-app shortcuts and profiles. Pairs to up to three devices and flows " +
                        "between them with cursor copy-paste.",
                99.00, ProductCategory.ACCESSORIES, Condition.NEW, 141, WeightUnit.GRAMS,
                STORE_RAID_ID, STORE_RAID_NAME, 45, 4.7, 612,
                colors("Black", "White"),
                imgs(MOUSE_1)));

        // ── Saifullah Store: 6 items ──────────────────────────────────
        products.add(p(now - (i++) * 60_000L,
                "Lenovo IdeaPad Slim 5 (Ryzen 7)",
                "Lenovo IdeaPad Slim 5 with AMD Ryzen 7 7730U, 16 GB DDR4, 512 GB NVMe SSD and a " +
                        "14\" 2.2K IPS display covering 100% sRGB. Aluminum lid, all-day battery " +
                        "(rated 13 hours), Dolby Atmos speakers and a fingerprint power button. " +
                        "Backlit keyboard, two USB-C and two USB-A ports, plus a full-size HDMI 1.4. " +
                        "Boxed with the 65W USB-C charger and Lenovo's standard 1-year warranty.",
                749.00, ProductCategory.LAPTOP, Condition.NEW, 1.5, WeightUnit.KILOGRAMS,
                STORE_SAIF_ID, STORE_SAIF_NAME, 14, 4.4, 76,
                colors("Silver", "Black"),
                imgs(LAPTOP_4, LAPTOP_5)));

        products.add(p(now - (i++) * 60_000L,
                "Google Pixel 8 128 GB",
                "The Pixel 8 packs Google's Tensor G3 with seven years of OS, security and Pixel " +
                        "feature drops. 6.2\" Actua OLED at up to 120 Hz, the Pixel camera with Magic " +
                        "Editor and Best Take, and a Titan M2 chip for hardware-backed security. " +
                        "Wireless charging, IP68, and a refined matte aluminum frame. Sold sealed; " +
                        "comes with USB-C to USB-C cable and a quick-start guide.",
                699.00, ProductCategory.SMARTPHONE, Condition.NEW, 187, WeightUnit.GRAMS,
                STORE_SAIF_ID, STORE_SAIF_NAME, 20, 4.6, 184,
                colors("Black", "Rose Gold", "Blue"),
                imgs(PHONE_5, PHONE_2)));

        products.add(p(now - (i++) * 60_000L,
                "Keychron K2 V2 Mechanical Keyboard",
                "75% layout, hot-swappable Gateron Brown switches, and a wireless option you can " +
                        "actually trust — Bluetooth 5.1 pairs with up to three devices. White LED " +
                        "backlight with 18 modes, ABS keycaps with shine-through legends. Type-C cable, " +
                        "Mac and Windows toggle on the side, and a 4,000 mAh battery rated for 240 hours " +
                        "of typing with the lights off. Aluminum frame option available on request.",
                89.00, ProductCategory.ACCESSORIES, Condition.NEW, 850, WeightUnit.GRAMS,
                STORE_SAIF_ID, STORE_SAIF_NAME, 35, 4.5, 142,
                colors("Black", "White"),
                imgs(KEYBOARD_1, KEYBOARD_2)));

        products.add(p(now - (i++) * 60_000L,
                "HP V24 24\" Full-HD Monitor",
                "Honest budget pick — 24-inch IPS at 1920×1080, 75 Hz with AMD FreeSync, and a " +
                        "5 ms response time that's perfectly fine for office work, study and casual " +
                        "gaming. Three-side micro-edge bezel, 178° viewing angles, and tilt adjustment. " +
                        "HDMI 1.4 + VGA inputs, 100×100 mm VESA mount support, and a low blue light mode " +
                        "for marathon sessions. Cables and stand included.",
                179.00, ProductCategory.MONITOR, Condition.NEW, 3.7, WeightUnit.KILOGRAMS,
                STORE_SAIF_ID, STORE_SAIF_NAME, 25, 4.3, 220,
                colors("Black"),
                imgs(MONITOR_3, MONITOR_2)));

        products.add(p(now - (i++) * 60_000L,
                "JBL Tune 510BT Wireless Headphones",
                "On-ear wireless headphones with JBL Pure Bass sound, 40-hour battery life and " +
                        "5-minute USB-C quick charging that gets you another 2 hours. Hands-free calls " +
                        "with the built-in mic, easy Bluetooth pairing with two devices, and convenient " +
                        "playback controls right on the earcup. Foldable design and lightweight enough " +
                        "to live in a daily bag.",
                49.00, ProductCategory.ACCESSORIES, Condition.NEW, 160, WeightUnit.GRAMS,
                STORE_SAIF_ID, STORE_SAIF_NAME, 50, 4.4, 388,
                colors("Black", "White", "Blue", "Purple"),
                imgs(HEADPHONES_2, HEADPHONES_3)));

        products.add(p(now - (i++) * 60_000L,
                "OnePlus 12 256 GB",
                "Snapdragon 8 Gen 3, 12 GB LPDDR5X RAM and a Hasselblad-tuned triple camera (50 MP " +
                        "main / 64 MP 3× periscope / 48 MP ultrawide) headline the OnePlus 12. " +
                        "6.82\" 2K LTPO 4.0 ProXDR display dims to 1 nit and peaks at 4,500 nits. " +
                        "5,400 mAh battery with 100W SuperVOOC wired and 50W AirVOOC wireless. " +
                        "OxygenOS 14 with four years of major Android updates committed.",
                799.00, ProductCategory.SMARTPHONE, Condition.NEW, 220, WeightUnit.GRAMS,
                STORE_SAIF_ID, STORE_SAIF_NAME, 16, 4.7, 158,
                colors("Black", "Green"),
                imgs(PHONE_4, PHONE_3)));

        // ── Organo: premium curation (6 items) ────────────────────────
        products.add(p(now - (i++) * 60_000L,
                "MacBook Air 15\" M3",
                "The 15-inch MacBook Air with M3 is the thin-and-light to beat — a fanless design, " +
                        "all-day 18-hour battery and a 15.3\" Liquid Retina display. Configured with " +
                        "an 8-core CPU, 10-core GPU, 16 GB unified memory and a 512 GB SSD. " +
                        "MagSafe charging, two Thunderbolt / USB 4 ports, and a 1080p FaceTime HD " +
                        "camera. Sealed in retail packaging with the 35W dual-port USB-C charger.",
                1299.00, ProductCategory.LAPTOP, Condition.NEW, 1.51, WeightUnit.KILOGRAMS,
                STORE_ORGANO_ID, STORE_ORGANO_NAME, 12, 4.9, 96,
                colors("Silver", "Gold", "Black"),
                imgs(LAPTOP_2, LAPTOP_1)));

        products.add(p(now - (i++) * 60_000L,
                "iPhone 15 128 GB",
                "iPhone 15 introduces the Dynamic Island, a 48 MP main camera and the colour-infused " +
                        "back glass that quietly stole the show. USB-C, the A16 Bionic chip from last " +
                        "year's Pro line, and a 6.1\" Super Retina XDR display with HDR up to 1,600 " +
                        "nits. Ceramic Shield front, IP68 water resistance, and Apple's standard one-year " +
                        "limited warranty. Sealed retail box with USB-C cable and documentation.",
                799.00, ProductCategory.SMARTPHONE, Condition.NEW, 171, WeightUnit.GRAMS,
                STORE_ORGANO_ID, STORE_ORGANO_NAME, 22, 4.7, 268,
                colors("Pink", "Blue", "Green", "Black"),
                imgs(PHONE_2, PHONE_1)));

        products.add(p(now - (i++) * 60_000L,
                "Apple Studio Display 27\" 5K",
                "5120×2880 5K Retina IPS at 600 nits, with True Tone and a wide P3 colour gamut. " +
                        "Built-in 12 MP Ultra Wide camera with Center Stage, a six-speaker spatial-audio " +
                        "system, and Studio-quality three-mic array. Single Thunderbolt 3 cable powers " +
                        "your MacBook (96W passthrough) and carries video. Tilt-adjustable stand " +
                        "included; tilt-and-height-adjustable available on request.",
                1599.00, ProductCategory.MONITOR, Condition.NEW, 6.3, WeightUnit.KILOGRAMS,
                STORE_ORGANO_ID, STORE_ORGANO_NAME, 8, 4.8, 64,
                colors("Silver"),
                imgs(MONITOR_2, MONITOR_1)));

        products.add(p(now - (i++) * 60_000L,
                "Apple Magic Keyboard with Touch ID",
                "Wireless mechanical scissor switches with a low-travel profile, plus the Touch ID " +
                        "sensor for fingerprint unlock and Apple Pay on Mac. Function row mapped to " +
                        "macOS shortcuts (Mission Control, Spotlight, Dictation). Rechargeable via " +
                        "USB-C, lasts about a month per charge, and pairs instantly when you plug it " +
                        "into a compatible Mac the first time.",
                129.00, ProductCategory.ACCESSORIES, Condition.NEW, 230, WeightUnit.GRAMS,
                STORE_ORGANO_ID, STORE_ORGANO_NAME, 30, 4.6, 144,
                colors("White", "Black"),
                imgs(KEYBOARD_1)));

        products.add(p(now - (i++) * 60_000L,
                "AirPods Max",
                "Over-ear AirPods with the H1 chip, computational audio, and a stainless-steel frame " +
                        "wrapped in a breathable knit-mesh canopy. Adaptive EQ, active noise " +
                        "cancellation and Spatial Audio with dynamic head tracking that'll spoil " +
                        "you for any other headphone. Digital Crown borrowed from Apple Watch for " +
                        "intuitive volume and playback. Includes Smart Case and a Lightning-to-USB-C cable.",
                549.00, ProductCategory.ACCESSORIES, Condition.NEW, 384.8, WeightUnit.GRAMS,
                STORE_ORGANO_ID, STORE_ORGANO_NAME, 10, 4.7, 88,
                colors("Silver", "Pink", "Green", "Blue"),
                imgs(HEADPHONES_1, HEADPHONES_2)));

        products.add(p(now - (i++) * 60_000L,
                "ASUS ROG Strix G16 (RTX 4070)",
                "16-inch FHD+ 165 Hz IPS gaming laptop with the Intel Core i9-13980HX and an NVIDIA " +
                        "RTX 4070 (140W TGP). 16 GB DDR5-4800 expandable to 32, a 1 TB Gen 4 NVMe SSD, " +
                        "and the ROG Intelligent Cooling system with liquid metal on the CPU. " +
                        "Per-key RGB keyboard, Wi-Fi 6E, and a 90 Wh battery. Includes the 280W brick " +
                        "and the slim ROG sleeve.",
                1799.00, ProductCategory.LAPTOP, Condition.NEW, 2.5, WeightUnit.KILOGRAMS,
                STORE_ORGANO_ID, STORE_ORGANO_NAME, 6, 4.7, 52,
                colors("Black", "Gray"),
                imgs(LAPTOP_3, LAPTOP_5)));

        return products;
    }

    /** 12 brand-new products, 4 per store, distinct from {@link #buildCatalog()}. */
    private static List<Product> buildExtraCatalog() {
        long now = System.currentTimeMillis();
        List<Product> products = new ArrayList<>();
        int i = 0;

        // ── Raid: 4 ───────────────────────────────────────────────────
        products.add(p(now - (i++) * 60_000L,
                "Dell XPS 15 OLED (RTX 4060)",
                "Dell's halo creator notebook — 15.6\" 3.5K OLED touch (3456×2160) covering 100% " +
                        "DCI-P3, paired with the Intel Core i7-13700H, 16 GB DDR5 and a 512 GB " +
                        "Gen 4 SSD. The RTX 4060 with 8 GB VRAM handles DaVinci Resolve, Blender " +
                        "iterations and the occasional AAA title at native 1080p. CNC-machined " +
                        "aluminium chassis, edge-to-edge keyboard with a haptic touchpad, and Killer " +
                        "Wi-Fi 6E. Includes the 130W USB-C charger.",
                1899.00, ProductCategory.LAPTOP, Condition.NEW, 1.92, WeightUnit.KILOGRAMS,
                STORE_RAID_ID, STORE_RAID_NAME, 9, 4.7, 64,
                colors("Silver", "Black"),
                imgs(LAPTOP_3, LAPTOP_5)));

        products.add(p(now - (i++) * 60_000L,
                "Xiaomi 14 Ultra 512 GB",
                "Xiaomi's Leica-tuned flagship goes wide — quad 50 MP cameras (Sony LYT-900 main " +
                        "with a variable f/1.63–f/4.0 aperture), Snapdragon 8 Gen 3, and a 6.73\" LTPO " +
                        "AMOLED at 3000×1440 / 120 Hz that hits 3,000 nits peak. Titanium-trim build, " +
                        "5,300 mAh battery with 90 W wired and 80 W wireless charging. HyperOS on top " +
                        "of Android 14, with four years of major OS updates committed.",
                1099.00, ProductCategory.SMARTPHONE, Condition.NEW, 219.8, WeightUnit.GRAMS,
                STORE_RAID_ID, STORE_RAID_NAME, 14, 4.6, 122,
                colors("Black", "White"),
                imgs(PHONE_3, PHONE_4)));

        products.add(p(now - (i++) * 60_000L,
                "LG UltraGear 32GR93U 32\" 4K Gaming Monitor",
                "32-inch 4K (3840×2160) IPS gaming display with native 144 Hz, 1 ms GtG response, " +
                        "and HDMI 2.1 (4K @ 120 Hz from PS5 / Xbox Series X). DisplayHDR 400, 95% DCI-P3, " +
                        "AMD FreeSync Premium and NVIDIA G-Sync Compatible. Tilt / height / pivot stand, " +
                        "VESA 100×100 mount, and the LG OnScreen Control software for picture profiles " +
                        "per app. Two HDMI 2.1, one DisplayPort 1.4, one USB-C (90 W PD).",
                799.00, ProductCategory.MONITOR, Condition.NEW, 7.4, WeightUnit.KILOGRAMS,
                STORE_RAID_ID, STORE_RAID_NAME, 7, 4.7, 48,
                colors("Black"),
                imgs(MONITOR_1, MONITOR_3)));

        products.add(p(now - (i++) * 60_000L,
                "Anker 737 Power Bank (PowerCore 24K)",
                "24,000 mAh of capacity, 140 W of total USB-C PD output split across two USB-C " +
                        "ports and one USB-A. The smart digital display shows remaining capacity, " +
                        "in/out wattage and a battery-health indicator down to a percent. Fast-charges " +
                        "a MacBook Pro 16, an iPhone, and AirPods all at once. TSA-friendly capacity " +
                        "for carry-on, and Anker's standard 24-month warranty applies.",
                149.00, ProductCategory.ACCESSORIES, Condition.NEW, 630, WeightUnit.GRAMS,
                STORE_RAID_ID, STORE_RAID_NAME, 25, 4.6, 312,
                colors("Black", "Silver"),
                imgs(POWERBANK_1)));

        // ── Saifullah Store: 4 ────────────────────────────────────────
        products.add(p(now - (i++) * 60_000L,
                "Acer Aspire 5 A515 (i5 12th Gen)",
                "An honest mid-range workhorse — Intel Core i5-1235U (10 cores, P+E), 8 GB DDR4 " +
                        "(one slot free for a stick of 16), and a 512 GB NVMe SSD. 15.6\" Full HD " +
                        "IPS panel with thin bezels, backlit keyboard, fingerprint reader on the " +
                        "power button, and a Wi-Fi 6 + Bluetooth 5.1 combo card. USB-C with " +
                        "DisplayPort, two USB-A, HDMI, and a microSD reader. Includes the 65 W " +
                        "barrel charger and Acer's 1-year international warranty.",
                549.00, ProductCategory.LAPTOP, Condition.NEW, 1.78, WeightUnit.KILOGRAMS,
                STORE_SAIF_ID, STORE_SAIF_NAME, 22, 4.4, 188,
                colors("Silver", "Black"),
                imgs(LAPTOP_4, LAPTOP_2)));

        products.add(p(now - (i++) * 60_000L,
                "Nothing Phone (2)",
                "Nothing's second-gen flagship runs the Snapdragon 8+ Gen 1, has a 6.7\" LTPO " +
                        "OLED at 1–120 Hz, and brings back the Glyph interface with 33 LED zones " +
                        "that handle ringtones, charging progress and Uber ETAs without touching the " +
                        "screen. Dual 50 MP cameras (Sony IMX890 main + Samsung JN1 ultrawide), " +
                        "4,700 mAh with 45 W wired and 15 W wireless. Nothing OS 2 ships clean — no " +
                        "third-party bloat, three years of OS updates committed.",
                599.00, ProductCategory.SMARTPHONE, Condition.NEW, 201.2, WeightUnit.GRAMS,
                STORE_SAIF_ID, STORE_SAIF_NAME, 18, 4.5, 142,
                colors("White", "Black"),
                imgs(PHONE_5, PHONE_1)));

        products.add(p(now - (i++) * 60_000L,
                "Razer DeathAdder V3 Pro Wireless",
                "Esports-grade wireless mouse — 64 g chassis (lightest DeathAdder ever), Razer " +
                        "Focus Pro 30K optical sensor, and HyperSpeed Wireless that's been LANs " +
                        "deep without packet loss. 90-hour battery on a single charge, 2nd-gen " +
                        "optical switches rated for 90 million clicks, and 100% PTFE feet for a " +
                        "buttery glide. Compatible with the Razer Mouse Dock Pro for true-wireless " +
                        "charging while you play.",
                149.00, ProductCategory.ACCESSORIES, Condition.NEW, 64, WeightUnit.GRAMS,
                STORE_SAIF_ID, STORE_SAIF_NAME, 30, 4.7, 256,
                colors("Black", "White"),
                imgs(MOUSE_1)));

        products.add(p(now - (i++) * 60_000L,
                "Samsung T7 Shield 1 TB Portable SSD",
                "Rugged USB 3.2 Gen 2 portable SSD — sustained reads up to 1,050 MB/s and writes " +
                        "up to 1,000 MB/s. The rubberised over-mould is rated IP65 (dust + water) " +
                        "and survives a 3-metre drop, with thermal throttling tuned so the drive " +
                        "doesn't slow down on long video transfers. AES 256-bit encryption available " +
                        "via the Samsung Magician app. Compatible with Mac, Windows, Android, " +
                        "PlayStation and Xbox out of the box. Includes both USB-C and USB-A cables.",
                89.00, ProductCategory.ACCESSORIES, Condition.NEW, 98, WeightUnit.GRAMS,
                STORE_SAIF_ID, STORE_SAIF_NAME, 40, 4.8, 410,
                colors("Black", "Blue"),
                imgs(SSD_1)));

        // ── Organo: 4 ─────────────────────────────────────────────────
        products.add(p(now - (i++) * 60_000L,
                "Microsoft Surface Laptop Studio 2",
                "Microsoft's most ambitious laptop — a 14.4\" 2400×1600 PixelSense Flow touch " +
                        "display at 120 Hz that pulls forward into Stage and Studio modes, paired " +
                        "with an Intel Core i7-13700H and the NVIDIA RTX 4060 (8 GB) for serious " +
                        "creative work. 16 GB LPDDR5x and a 512 GB SSD. Surface Pen 2 support with " +
                        "Zero Force inking, two Thunderbolt 4 ports, and the haptic Precision " +
                        "trackpad with adjustable click force. Boxed with the 102 W Surface Connect charger.",
                2399.00, ProductCategory.LAPTOP, Condition.NEW, 1.98, WeightUnit.KILOGRAMS,
                STORE_ORGANO_ID, STORE_ORGANO_NAME, 5, 4.7, 32,
                colors("Black"),
                imgs(LAPTOP_5, LAPTOP_3)));

        products.add(p(now - (i++) * 60_000L,
                "Samsung Galaxy Z Fold5 512 GB",
                "The thinnest, lightest Z Fold yet — Snapdragon 8 Gen 2 for Galaxy, the new " +
                        "Flex Hinge that closes flush with no visible gap, and a 7.6\" Dynamic AMOLED " +
                        "2X main display with 120 Hz. Triple-camera with a 50 MP main, 12 MP ultrawide " +
                        "and a 10 MP 3× telephoto. S Pen-ready (case sold separately), 4,400 mAh " +
                        "battery, and Samsung's 4-year OS update commitment. IPX8 water resistance.",
                1799.00, ProductCategory.SMARTPHONE, Condition.NEW, 253, WeightUnit.GRAMS,
                STORE_ORGANO_ID, STORE_ORGANO_NAME, 7, 4.6, 56,
                colors("Black", "Silver", "Blue"),
                imgs(PHONE_4, PHONE_3)));

        products.add(p(now - (i++) * 60_000L,
                "LG UltraFine 5K 27\" Monitor",
                "27-inch 5120×2880 IPS panel with a wide P3 colour gamut and 500 nits typical " +
                        "brightness — built in collaboration with Apple to be the natural pairing " +
                        "for a MacBook Pro. Single Thunderbolt 3 cable carries 5K video and 94 W " +
                        "of charging both ways. Three downstream USB-C ports, an integrated 12 MP " +
                        "camera and stereo speakers, and a tilt/height-adjustable arm with a 5-year " +
                        "warranty on the panel.",
                1299.00, ProductCategory.MONITOR, Condition.NEW, 7.6, WeightUnit.KILOGRAMS,
                STORE_ORGANO_ID, STORE_ORGANO_NAME, 6, 4.6, 38,
                colors("Silver"),
                imgs(MONITOR_2, MONITOR_1)));

        products.add(p(now - (i++) * 60_000L,
                "Bose QuietComfort Ultra Earbuds",
                "Bose's flagship in-ears — Immersive Audio with head-tracked spatial sound, the " +
                        "best-in-class active noise cancellation Bose is known for, and the new " +
                        "CustomTune system that calibrates the audio profile to your ear canals every " +
                        "time you put them in. Six-hour battery in the buds, 24 with the case, USB-C " +
                        "and wireless charging. Multipoint Bluetooth pairs to two devices at once. " +
                        "Includes three sizes of silicone ear tips and two stability bands.",
                299.00, ProductCategory.ACCESSORIES, Condition.NEW, 60, WeightUnit.GRAMS,
                STORE_ORGANO_ID, STORE_ORGANO_NAME, 18, 4.7, 196,
                colors("Black", "White"),
                imgs(EARBUDS_1, HEADPHONES_3)));

        return products;
    }

    private static Product p(long createdAt, String name, String description, double price,
                             ProductCategory cat, Condition cond, double weight, WeightUnit wu,
                             String storeId, String storeName, int stock, double rating, int sold,
                             List<Color> colors, List<String> images) {
        Product p = new Product();
        p.setName(name);
        p.setDescription(description);
        p.setPrice(price);
        p.setCategory(cat);
        p.setCondition(cond);
        p.setWeight(weight);
        p.setWeightUnit(wu);
        p.setStoreId(storeId);
        p.setStoreName(storeName);
        p.setLocation(LOCATION);
        p.setStock(stock);
        p.setRating(rating);
        p.setItemsSold(sold);
        p.setAvailableColors(colors);
        p.setImageUrls(images);
        p.setCreatedAt(createdAt);
        return p;
    }

    @NonNull
    private static List<String> imgs(@NonNull String... urls) {
        return new ArrayList<>(Arrays.asList(urls));
    }

    @NonNull
    private static List<Color> colors(@NonNull String... names) {
        List<Color> out = new ArrayList<>();
        for (String n : names) {
            String hex = PALETTE.get(n);
            if (hex == null) continue;
            out.add(new Color(n, hex));
        }
        if (out.isEmpty()) out.add(new Color("Black", "#1C1C1E"));
        return out;
    }

    /** Mirrors the curated palette in {@code AddProductActivity} so colours stay consistent. */
    private static final Map<String, String> PALETTE;
    static {
        Map<String, String> m = new HashMap<>();
        m.put("Black",     "#1C1C1E");
        m.put("White",     "#FFFFFF");
        m.put("Silver",    "#C0C0C0");
        m.put("Gold",      "#D4AF37");
        m.put("Rose Gold", "#B76E79");
        m.put("Blue",      "#2D7DFF");
        m.put("Red",       "#E53935");
        m.put("Green",     "#2E7D32");
        m.put("Purple",    "#7B3FA0");
        m.put("Pink",      "#EC407A");
        m.put("Gray",      "#5A5A5A");
        PALETTE = Collections.unmodifiableMap(m);
    }

    // ──────────────────────────── Image URLs ────────────────────────────
    // Public Unsplash photos — picked by category, not exact SKU.

    private static final String LAPTOP_1     = "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=800&q=80&auto=format&fit=crop";
    private static final String LAPTOP_2     = "https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=800&q=80&auto=format&fit=crop";
    private static final String LAPTOP_3     = "https://images.unsplash.com/photo-1541807084-5c52b6b3adef?w=800&q=80&auto=format&fit=crop";
    private static final String LAPTOP_4     = "https://images.unsplash.com/photo-1531297484001-80022131f5a1?w=800&q=80&auto=format&fit=crop";
    private static final String LAPTOP_5     = "https://images.unsplash.com/photo-1588872657578-7efd1f1555ed?w=800&q=80&auto=format&fit=crop";

    private static final String PHONE_1      = "https://images.unsplash.com/photo-1592899677977-9c10ca588bbd?w=800&q=80&auto=format&fit=crop";
    private static final String PHONE_2      = "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=800&q=80&auto=format&fit=crop";
    private static final String PHONE_3      = "https://images.unsplash.com/photo-1601784551446-20c9e07cdbdb?w=800&q=80&auto=format&fit=crop";
    private static final String PHONE_4      = "https://images.unsplash.com/photo-1574944985070-8f3ebc6b79d2?w=800&q=80&auto=format&fit=crop";
    private static final String PHONE_5      = "https://images.unsplash.com/photo-1605236453806-6ff36851218e?w=800&q=80&auto=format&fit=crop";

    private static final String MONITOR_1    = "https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=800&q=80&auto=format&fit=crop";
    private static final String MONITOR_2    = "https://images.unsplash.com/photo-1593640408182-31c70c8268f5?w=800&q=80&auto=format&fit=crop";
    private static final String MONITOR_3    = "https://images.unsplash.com/photo-1547119957-637f8679db1e?w=800&q=80&auto=format&fit=crop";

    private static final String HEADPHONES_1 = "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800&q=80&auto=format&fit=crop";
    private static final String HEADPHONES_2 = "https://images.unsplash.com/photo-1546435770-a3e426bf472b?w=800&q=80&auto=format&fit=crop";
    private static final String HEADPHONES_3 = "https://images.unsplash.com/photo-1583394838336-acd977736f90?w=800&q=80&auto=format&fit=crop";

    private static final String KEYBOARD_1   = "https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=800&q=80&auto=format&fit=crop";
    private static final String KEYBOARD_2   = "https://images.unsplash.com/photo-1561112078-7d24e04c3407?w=800&q=80&auto=format&fit=crop";

    private static final String MOUSE_1      = "https://images.unsplash.com/photo-1527814050087-3793815479db?w=800&q=80&auto=format&fit=crop";

    private static final String POWERBANK_1  = "https://images.unsplash.com/photo-1609692814859-e2b1d6c84517?w=800&q=80&auto=format&fit=crop";
    private static final String SSD_1        = "https://images.unsplash.com/photo-1531492746076-161ca9bcad58?w=800&q=80&auto=format&fit=crop";
    private static final String EARBUDS_1    = "https://images.unsplash.com/photo-1606220588913-b3aacb4d2f46?w=800&q=80&auto=format&fit=crop";
}
