import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

// One-off generator for the RuneLite navigation icon shown on the right toolbar.
// Run via:
//   javac scripts/GenerateIcon.java && \
//     java -cp scripts GenerateIcon \
//       src/main/resources/com/smicalexshiao/leaguetasks/checklist.png
public final class GenerateIcon
{
	public static void main(String[] args) throws Exception
	{
		if (args.length < 1)
		{
			System.err.println("usage: GenerateIcon <output.png>");
			System.exit(2);
		}

		int size = 32;
		BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g.setComposite(AlphaComposite.Src);

		// Color palette tuned to read on RuneLite's dark sidebar:
		// cream board, RuneLite brand orange highlights, dark outline.
		Color board = new Color(0xE8C994);
		Color boardEdge = new Color(0x2A2A2A);
		Color clip = new Color(0x8B5A2B);
		Color tick = new Color(0xFF8C00);
		Color line = new Color(0x3A2E22);

		// Clipboard body — rounded rect.
		int bx = 5;
		int by = 6;
		int bw = 22;
		int bh = 24;
		g.setColor(board);
		g.fillRoundRect(bx, by, bw, bh, 4, 4);
		g.setColor(boardEdge);
		g.setStroke(new BasicStroke(1.5f));
		g.drawRoundRect(bx, by, bw, bh, 4, 4);

		// Clip at the top.
		int cx = 11;
		int cy = 3;
		int cw = 10;
		int ch = 6;
		g.setColor(clip);
		g.fillRoundRect(cx, cy, cw, ch, 2, 2);
		g.setColor(boardEdge);
		g.drawRoundRect(cx, cy, cw, ch, 2, 2);

		// Three list rows: top two are checked off, third is open.
		int rowX = 8;
		int rowY = 13;
		int rowGap = 6;
		for (int i = 0; i < 3; i++)
		{
			int y = rowY + i * rowGap;

			// Item line on the right of the checkbox.
			g.setColor(line);
			g.fillRect(rowX + 6, y + 1, 12, 2);

			if (i < 2)
			{
				// Checkmark stroke (no surrounding box, reads cleaner at 32px).
				g.setColor(tick);
				g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				Path2D.Float check = new Path2D.Float();
				check.moveTo(rowX, y + 2);
				check.lineTo(rowX + 1.5f, y + 3.5f);
				check.lineTo(rowX + 4.5f, y);
				g.draw(check);
			}
			else
			{
				// Open checkbox for the last item.
				g.setColor(boardEdge);
				g.setStroke(new BasicStroke(1.0f));
				g.drawRect(rowX, y, 4, 4);
			}
		}

		g.dispose();
		File out = new File(args[0]);
		ImageIO.write(img, "PNG", out);
		System.out.println("Wrote " + out.getAbsolutePath() + " (" + out.length() + " bytes)");
	}
}
