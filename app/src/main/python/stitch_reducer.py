# import math
# import os
# from pyembroidery import EmbPattern, read_dst, END, STITCH, write_png

# # --- Configuration ---
# # The smallest stitch length to keep, in 1/10mm units.
# # Stitches shorter than this will be removed.
# # Tajima (.dst) format has a resolution of 0.1mm.
# # A value of 5 means 0.5mm.
# MINIMUM_STITCH_LENGTH = 15  # 1.7mm in 1/10mm units

# def calculate_distance(p1, p2):
#     """Calculates the Euclidean distance between two points."""
#     return math.sqrt((p2[0] - p1[0])**2 + (p2[1] - p1[1])**2)

# def optimize_pattern(input_path, output_path):
#     """
#     Reads a DST file, removes small stitches, and saves the optimized
#     pattern to a new DST file.
#     """
#     # Check if the input file exists
#     if not os.path.exists(input_path):
#         print(f"Error: The file '{input_path}' was not found.")
#         return

#     # Load the embroidery pattern from the DST file
#     try:
#         pattern = read_dst(input_path)
#     except Exception as e:
#         print(f"Error reading the DST file: {e}")
#         return
    
#     if not pattern:
#         print(f"Error: Could not read the file at {input_path}")
#         return

#     # Get the list of stitches from the original pattern
#     stitches = pattern.stitches
    
#     if not stitches:
#         print("The pattern contains no stitches to optimize.")
#         return

#     # Create a new list to hold the optimized stitches
#     optimized_stitches = []
    
#     if stitches:
#         # Add the first stitch or command to the list to start
#         optimized_stitches.append(stitches[0])
#         last_kept_stitch = stitches[0]

#     # Iterate through the rest of the stitches to filter them
#     for i in range(1, len(stitches)):
#         current_stitch = stitches[i]
        
#         # --- THIS IS THE CORRECTED LOGIC ---
#         # Safely check if the command is a simple STITCH command (integer 0).
#         # This prevents TypeErrors on complex commands where stitch[2] can be a list.
#         is_stitch_command = (
#             isinstance(last_kept_stitch[2], int) and last_kept_stitch[2] == STITCH and
#             isinstance(current_stitch[2], int) and current_stitch[2] == STITCH
#         )

#         if is_stitch_command:
#             distance = calculate_distance(
#                 (last_kept_stitch[0], last_kept_stitch[1]), 
#                 (current_stitch[0], current_stitch[1])
#             )
            
#             # If the stitch is too small, skip adding it.
#             # We do NOT update last_kept_stitch, so the next stitch is compared
#             # to the last one we actually kept.
#             if distance < MINIMUM_STITCH_LENGTH:
#                 continue
        
#         # Add the valid stitch or command to our new list
#         optimized_stitches.append(current_stitch)
#         last_kept_stitch = current_stitch
    
#     # Create a new pattern object
#     optimized_pattern = EmbPattern()
#     # Assign our newly created, filtered list of stitches to it
#     optimized_pattern.stitches = optimized_stitches
#     # Add the final END command
#     optimized_pattern.add_command(END)

#     # Save the optimized pattern to the new DST file
#     optimized_pattern.write(output_path)
#     png_path = output_path.replace('.dst', '.png')
#     try:
#         # Create PNG preview from the optimized pattern
#         write_png(optimized_pattern, png_path)
#         print(f"PNG preview saved to: {png_path}")
#     except Exception as e:
#         print(f"Warning: Could not generate PNG preview: {e}")
#         # Still return success for DST optimization even if PNG fails
#         png_path = None
#     original_count = len(stitches)
#     new_count = len(optimized_pattern.stitches)
    
#     print("\nOptimization Complete.")
#     print(f"Original stitch count: {original_count}")
#     print(f"Optimized stitch count: {new_count}")
#     print(f"Reduced by: {original_count - new_count} stitches")
#     print(f"Optimized file saved to: {output_path}")
#     return {
#         "status": "success",
#         "original_count": original_count,
#         "new_count": new_count,
#         "output_path": str(output_path),
#         "png_path": str(png_path) if png_path else None,
#         "algorithm": "adaptive_stitch_reduction",
#         "message": f"Reduced by: {original_count - new_count} stitches",
#     }
import math
import os
from pyembroidery import EmbPattern, read_dst, END, STITCH, write_png

# --- Configuration ---
# The smallest stitch length to keep, in 1/10mm units.
# Stitches shorter than this will be removed.
# Tajima (.dst) format has a resolution of 0.1mm.
# A value of 5 means 0.5mm.
MINIMUM_STITCH_LENGTH = 15  # 1.7mm in 1/10mm units

def calculate_distance(p1, p2):
    """Calculates the Euclidean distance between two points."""
    return math.sqrt((p2[0] - p1[0])**2 + (p2[1] - p1[1])**2)

def optimize_pattern(input_path, output_filename):
    """
    Reads a DST file, removes small stitches, and saves the optimized
    pattern to a new DST file.
    """
    try:
        # Use the same directory as input file for output
        input_dir = os.path.dirname(input_path)
        output_path = os.path.join(input_dir, output_filename)
        
        print(f"Input path: {input_path}")
        print(f"Output path: {output_path}")
        
        # Check if the input file exists
        if not os.path.exists(input_path):
            print(f"Error: The file '{input_path}' was not found.")
            return {
                "status": "error",
                "message": f"The file '{input_path}' was not found."
            }

        # Load the embroidery pattern from the DST file
        try:
            pattern = read_dst(input_path)
        except Exception as e:
            print(f"Error reading the DST file: {e}")
            return {
                "status": "error",
                "message": f"Error reading the DST file: {e}"
            }
        
        if not pattern:
            print(f"Error: Could not read the file at {input_path}")
            return {
                "status": "error",
                "message": f"Could not read the file at {input_path}"
            }

        # Get the list of stitches from the original pattern
        stitches = pattern.stitches
        
        if not stitches:
            print("The pattern contains no stitches to optimize.")
            return {
                "status": "error",
                "message": "The pattern contains no stitches to optimize."
            }

        # Create a new list to hold the optimized stitches
        optimized_stitches = []
        
        if stitches:
            # Add the first stitch or command to the list to start
            optimized_stitches.append(stitches[0])
            last_kept_stitch = stitches[0]

        # Iterate through the rest of the stitches to filter them
        for i in range(1, len(stitches)):
            current_stitch = stitches[i]
            
            # --- THIS IS THE CORRECTED LOGIC ---
            # Safely check if the command is a simple STITCH command (integer 0).
            # This prevents TypeErrors on complex commands where stitch[2] can be a list.
            is_stitch_command = (
                isinstance(last_kept_stitch[2], int) and last_kept_stitch[2] == STITCH and
                isinstance(current_stitch[2], int) and current_stitch[2] == STITCH
            )

            if is_stitch_command:
                distance = calculate_distance(
                    (last_kept_stitch[0], last_kept_stitch[1]), 
                    (current_stitch[0], current_stitch[1])
                )
                
                # If the stitch is too small, skip adding it.
                # We do NOT update last_kept_stitch, so the next stitch is compared
                # to the last one we actually kept.
                if distance < MINIMUM_STITCH_LENGTH:
                    continue
            
            # Add the valid stitch or command to our new list
            optimized_stitches.append(current_stitch)
            last_kept_stitch = current_stitch
        
        # Create a new pattern object
        optimized_pattern = EmbPattern()
        # Assign our newly created, filtered list of stitches to it
        optimized_pattern.stitches = optimized_stitches
        # Add the final END command
        optimized_pattern.add_command(END)

        # Save the optimized pattern to the new DST file
        try:
            optimized_pattern.write(output_path)
            
            # Verify the file was created
            if not os.path.exists(output_path):
                return {
                    "status": "error",
                    "message": f"Failed to create output file: {output_path}"
                }
            
            # Check if file has content
            if os.path.getsize(output_path) == 0:
                return {
                    "status": "error",
                    "message": f"Output file created but is empty: {output_path}"
                }
                
        except Exception as e:
            return {
                "status": "error",
                "message": f"Error writing DST file: {str(e)}"
            }

        # Generate PNG preview
        png_path = output_path.replace('.dst', '.png').replace('.DST', '.png')
        try:
            # Create PNG preview from the optimized pattern
            write_png(optimized_pattern, png_path)
            print(f"PNG preview saved to: {png_path}")
            
            # Verify PNG was created
            if not os.path.exists(png_path):
                png_path = None
                
        except Exception as e:
            print(f"Warning: Could not generate PNG preview: {e}")
            # Still return success for DST optimization even if PNG fails
            png_path = None

        original_count = len(stitches)
        new_count = len(optimized_pattern.stitches)
        
        print("\nOptimization Complete.")
        print(f"Original stitch count: {original_count}")
        print(f"Optimized stitch count: {new_count}")
        print(f"Reduced by: {original_count - new_count} stitches")
        print(f"Optimized file saved to: {output_path}")
        
        return {
            "status": "success",
            "original_count": original_count,
            "new_count": new_count,
            "output_path": str(output_path),
            "png_path": str(png_path) if png_path else None,
            "algorithm": "adaptive_stitch_reduction",
            "message": f"Reduced by: {original_count - new_count} stitches",
        }
        
    except Exception as e:
        print(f"Unexpected error during processing: {str(e)}")
        return {
            "status": "error",
            "message": f"Unexpected error during processing: {str(e)}"
        }


def preview_dst(input_path):
    """
    Generates a preview image and metadata for a DST embroidery file.
    """
    try:
        print(f"Generating preview for: {input_path}")
        
        if not os.path.exists(input_path):
            return {
                "status": "error",
                "message": f"Input file not found: {input_path}"
            }
        
        # Get file size
        file_size = os.path.getsize(input_path)
        
        # Load the embroidery pattern
        try:
            pattern = read_dst(input_path)
        except Exception as e:
            return {
                "status": "error",
                "message": f"Error reading DST file: {str(e)}"
            }
        
        if not pattern:
            return {
                "status": "error",
                "message": f"Could not read the file at {input_path}"
            }
        
        # Get pattern metadata
        stitches = pattern.stitches
        stitch_count = len(stitches) if stitches else 0
        
        # Calculate bounds
        if stitches:
            x_coords = [stitch[0] for stitch in stitches if len(stitch) >= 2]
            y_coords = [stitch[1] for stitch in stitches if len(stitch) >= 2]
            
            if x_coords and y_coords:
                width = max(x_coords) - min(x_coords)
                height = max(y_coords) - min(y_coords)
                bounds = f"{width/10:.1f}mm × {height/10:.1f}mm"  # Convert to mm
            else:
                bounds = "Unknown"
        else:
            bounds = "No stitches"
        
        # Generate PNG preview
        input_dir = os.path.dirname(input_path)
        input_filename = os.path.basename(input_path)
        png_filename = f"preview_{input_filename.replace('.dst', '.png').replace('.DST', '.png')}"
        png_path = os.path.join(input_dir, png_filename)
        
        try:
            write_png(pattern, png_path)
            
            # Verify PNG was created
            if not os.path.exists(png_path) or os.path.getsize(png_path) == 0:
                png_path = None
        except Exception as e:
            print(f"Warning: Could not generate PNG preview: {e}")
            png_path = None
        
        print(f"Preview generated successfully")
        
        return {
            "status": "success",
            "stitch_count": stitch_count,
            "file_size": file_size,
            "file_path": str(input_path),
            "bounds": bounds,
            "png_path": str(png_path) if png_path else None,
            "message": "Preview generated successfully"
        }
        
    except Exception as e:
        print(f"Error generating preview: {str(e)}")
        return {
            "status": "error",
            "message": f"Error generating preview: {str(e)}"
        }
