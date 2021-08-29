package svenhjol.strange.module.journals.screen;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import svenhjol.charm.helper.LogHelper;
import svenhjol.charm.helper.WorldHelper;
import svenhjol.charm.mixin.accessor.ScreenAccessor;
import svenhjol.strange.module.journals.Journals;
import svenhjol.strange.module.journals.JournalsClient;
import svenhjol.strange.module.journals.data.JournalLocation;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("ConstantConditions")
public class JournalLocationScreen extends BaseJournalScreen {
    protected static final int MAX_NAME_LENGTH = 50;
    protected static final int MIN_PHOTO_DISTANCE = 10;
    protected String name;
    protected EditBox nameField;
    protected JournalLocation location;
    protected DynamicTexture photoTexture = null;
    protected ResourceLocation registeredPhotoTexture = null;

    protected int photoFailureRetries = 0;
    protected boolean hasInitializedUpdateButtons = false;
    protected boolean hasRenderedUpdateButtons = false;
    protected boolean hasPhoto = false;
    protected List<ItemLike> icons = new ArrayList<>();
    protected List<ButtonDefinition> updateButtons = new ArrayList<>();

    public JournalLocationScreen(JournalLocation location) {
        super(new TextComponent(location.getName()));

        this.name = location.getName();
        this.location = location.copy();
    }

    @Override
    protected void init() {
        super.init();

        if (minecraft == null)
            return;

        // set up the input field for editing the entry name
        nameField = new EditBox(font, (width / 2) + 5, 40, 105, 12, new TextComponent("NameField"));
        nameField.changeFocus(true);
        nameField.setCanLoseFocus(false);
        nameField.setTextColor(-1);
        nameField.setTextColorUneditable(-1);
        nameField.setBordered(true);
        nameField.setMaxLength(MAX_NAME_LENGTH);
        nameField.setResponder(this::nameFieldResponder);
        nameField.setValue(name);
        nameField.setEditable(true);

        minecraft.keyboardHandler.setSendRepeatsToGui(true);
        ((ScreenAccessor)this).getChildren().add(nameField);
        setFocused(nameField);

        if (!hasInitializedUpdateButtons) {
            // add a back button at the bottom
            bottomButtons.add(0, new ButtonDefinition(b -> saveAndGoBack(),
                new TranslatableComponent("gui.strange.journal.go_back")));

            // if player has empty map, add button to make a map of this location
            if (playerHasEmptyMap()) {
                updateButtons.add(
                    new ButtonDefinition(b -> makeMap(),
                        new TranslatableComponent("gui.strange.journal.make_map"))
                );
            }

            // if player is near the location, add button to take a photo
            if (playerIsNearLocation()) {
                updateButtons.add(
                    new ButtonDefinition(b -> takePhoto(),
                        new TranslatableComponent("gui.strange.journal.take_photo"))
                );
            }

            // always add an icon button
            updateButtons.add(
                new ButtonDefinition(b -> chooseIcon(),
                    new TranslatableComponent("gui.strange.journal.choose_icon"))
            );

            // always add a save button
            updateButtons.add(
                new ButtonDefinition(b -> saveAndGoBack(),
                    new TranslatableComponent("gui.strange.journal.save"))
            );

            hasInitializedUpdateButtons = true;
        }

        hasRenderedUpdateButtons = false;
        hasPhoto = hasPhoto();
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float delta) {
        super.render(poseStack, mouseX, mouseY, delta);
        int mid = width / 2;

        // render icon next to title
        renderTitleIcon(location.getIcon());


        // render left-side page
        if (hasPhoto) {
            renderPhoto(poseStack);
        } else {
            // TODO: show button to take photo here?
        }

        // render right-side buttons
        if (!hasRenderedUpdateButtons) {
            int buttonWidth = 105;
            int buttonHeight = 20;
            int left = mid + 5;
            int yStart = 150;
            int yOffset = -22;

            renderButtons(updateButtons, left, yStart, 0, yOffset, buttonWidth, buttonHeight);
            hasRenderedUpdateButtons = true;
        }

        nameField.render(poseStack, mouseX, mouseY, delta);
    }

    protected void renderPhoto(PoseStack poseStack) {
        if (minecraft == null)
            return;

        if (photoTexture == null) {
            try {
                File photoFile = getPhoto();
                if (photoFile == null)
                    throw new Exception("Null problems with file");

                RandomAccessFile raf = new RandomAccessFile(photoFile, "r");
                if (raf != null)
                    raf.close();

                InputStream stream = new FileInputStream(photoFile);
                NativeImage photo = NativeImage.read(stream);
                photoTexture = new DynamicTexture(photo);
                registeredPhotoTexture = minecraft.getTextureManager().register("photo", photoTexture);
                stream.close();

                if (photoTexture == null || registeredPhotoTexture == null)
                    throw new Exception("Null problems with texture / registered texture");

            } catch (Exception e) {
                LogHelper.warn(this.getClass(), "Error loading photo: " + e);
                photoFailureRetries++;

                if (photoFailureRetries > 2) {
                    LogHelper.error(getClass(), "Failure loading photo, aborting retries");
                    hasPhoto = false;
                    registeredPhotoTexture = null;
                    photoTexture = null;
                }
            }
        }

        if (registeredPhotoTexture != null) {
            RenderSystem.setShaderTexture(0, registeredPhotoTexture);
            poseStack.pushPose();
            poseStack.scale(0.425F, 0.33F, 0.425F);
            blit(poseStack, ((int)((width / 2) / 0.425F) - 265), 120, 0, 0, 256, 200);

            poseStack.popPose();
        }
    }

    /**
     * Add an area of the page that allows photo to be clicked.
     */
    @Override
    public boolean mouseClicked(double x, double y, int button) {
        int mid = width / 2;

        if (hasPhoto && registeredPhotoTexture != null) {
            if (x > (mid - 112) && x < mid - 3
                && y > 39 && y < 106) {
                minecraft.setScreen(new JournalPhotoScreen(location));
                return true;
            }
        }

        return super.mouseClicked(x, y, button);
    }

    /**
     * We need to resync the journal when leaving this page to go back to locations.
     */
    protected void saveAndGoBack() {
        save(); // save progress before changing screen
        JournalsClient.sendOpenJournal(Journals.Page.LOCATIONS);
    }

    protected void save() {
        JournalsClient.sendUpdateLocation(location);
    }

    protected void chooseIcon() {
        save(); // save progress before changing screen
        if (minecraft != null)
            minecraft.setScreen(new JournalChooseIconScreen(location));
    }

    protected void makeMap() {

    }

    protected void takePhoto() {
        if (minecraft != null && minecraft.player != null) {
            minecraft.setScreen(null);
            minecraft.options.hideGui = true;
            JournalsClient.locationBeingPhotographed = location;
            JournalsClient.photoTicks = 1;
        }
    }

    @Nullable
    protected File getPhoto() {
        if (minecraft == null)
            return null;

        File screenshotsDirectory = new File(minecraft.gameDirectory, "screenshots");
        return new File(screenshotsDirectory, location.getId() + ".png");
    }

    private boolean hasPhoto() {
        File photo = getPhoto();
        return photo != null && photo.exists();
    }

    protected boolean playerHasEmptyMap() {
        if (minecraft != null && minecraft.player != null)
            return minecraft.player.getInventory().contains(new ItemStack(Items.MAP));

        return false;
    }

    protected boolean playerIsNearLocation() {
        if (minecraft != null && minecraft.player != null)
            return WorldHelper.getDistanceSquared(minecraft.player.blockPosition(), location.getBlockPos()) < MIN_PHOTO_DISTANCE;

        return false;
    }

    private void nameFieldResponder(String text) {
        location.setName(text);
    }
}
