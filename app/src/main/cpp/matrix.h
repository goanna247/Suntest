//
// Created by goann on 6/06/2024.
//

#ifndef LIBTEST_MATRIX_H
#define LIBTEST_MATRIX_H




////////////////////////////////////////////////////////////////////////////////
// Configuration.

/// Compile time option to enable test functions. Force disabled if no debug.
#undef MATRIX_UNIT_TEST

#if !defined(DEBUG)
#undef MATRIX_UNIT_TEST
#endif



////////////////////////////////////////////////////////////////////////////////
// Constants.

#define MATRIX_EPSILON_DEFAULT		1e-12		///< Default value for matrix_epsilon



////////////////////////////////////////////////////////////////////////////////
/// Enumeration of matrix error codes.

typedef enum
{
    matrixOk = 0,					///< No error occured.
    matrixErrBadArg,				///< Argument to functionis invalid. Usually a matrix is NULL or has no elements.
    matrixErrBadIndex,				///< The specified row and/or column was out of bounds for the matrix or row/column comination of arguments not valid for operation.
    matrixErrNotSquare,				///< The matrix is not square.
    matrixErrSingular,				///< The matrix is singular.
    matrixErrZeroNotAllowed,		///< Value of zero not allowed for parameter. Used for ERO multiplier.
    matrixErrMemAllocation,			///< Failed to allocate memory for matrix.
} matrix_err_t;




////////////////////////////////////////////////////////////////////////////////
/// Container object used to hold a series of data points used for OLS minimisation.

typedef struct
{
    int		rows;			///< Number of rows that the matrix contains.
    int		cols;			///< Number of columns that the matrix contains.
    double *element;		///< Pointer to array elements stored in row-major format
} matrix_t;



////////////////////////////////////////////////////////////////////////////////
/// Value of epsilon for matrix computations. Determinants whose absolute values
/// are less than or equial to matrix_epsilon are considered zero and therefore
/// the corresponding matrix is singular and non invertable. A multiplier for a
/// elementary row operation must not be within epsilon of zero. Unit test results
/// must be within matrix_epsilon (inclusive) to be considered successful.
/// May be set by application. Defaults to MATRIX_EPSILON_DEFAULT.

extern double matrix_epsilon;



////////////////////////////////////////////////////////////////////////////////
/// Creates a matrix of specified size. The number of rows and columns may be
/// zero but elements of the matrix will not be accessable until its size is
/// fixed. It may be fixed using matrix_set_size() or when it is the destination
/// of an operation on one or more other matrices. Elements of the matrix will
/// be initialised to 0.0.
///
/// \brief Create a matrix.
/// \param rows The number of rows for the new matrix. May be zero.
/// \param cols The number of columns for the new matrix. May be zero.
/// \return Pointer to new matrix or NULL on failure.

matrix_t *matrix_create(int rows, int cols);



////////////////////////////////////////////////////////////////////////////////
/// Destoys a matrix and all related resources.
///
/// \brief Destroy a matrix.
/// \param matrix Pointer to the matrix to be destroyed. If \p matrix is NULL,
/// no action is taken.

void matrix_destroy(matrix_t *matrix);



////////////////////////////////////////////////////////////////////////////////
/// Creates a duplicate of specified matrix with its own copy of elements.
///
/// \brief Duplicate a matrix.
/// \param matrix Pointer to the matrix to be duplicated.
/// \return Pointer to new matrix that is a duplicate of the specified matrix.
/// Will return NULL if \p matrix was NULL or a memory allocation failure occured.

matrix_t *matrix_duplicate(const matrix_t *matrix);



////////////////////////////////////////////////////////////////////////////////
/// Copies an array of double values into the elements of specified matrix.
/// assumes the array is in row major format.
///
/// \brief Load matrix with array of double values.
/// \param matrix Pointer to the matrix to be loaded.
/// \param data Pointer to an array of doubles holding the elements to be loaded
/// into \p matrix in row major format.
/// \param n Number of doubles contained in \p data.
/// \return Error code.
/// \retval matrixOk No error. Elements laoded OK.
/// \retval matrixErrBadArg \p matrix was NULL.
/// \retval matrixErrBadIndex \p n was smaller than the number of elements that \p matrix holds.

matrix_err_t matrix_load(matrix_t *matrix, const double *data, size_t n);



////////////////////////////////////////////////////////////////////////////////
/// Returns the value of a matrix element at specified row and column.
///
/// \brief Get matrix element.
/// \param matrix Pointer to the matrix.
/// \param row The row of the required element indexed starting from zero.
/// \param col The column of the required element indexed starting from zero.
/// \return Value of the specified element. Will return 0.0 if \p matrix was
/// NULL or the row/col was out of range.

double matrix_get_element(matrix_t *matrix, int row, int col);



////////////////////////////////////////////////////////////////////////////////
/// Modifies the value of a matrix element at specified row and column.
///
/// \brief Set matrix element.
/// \param matrix Pointer to the matrix.
/// \param row The row of the element to be modified indexed starting from zero.
/// \param col The column of the element to be modified indexed starting from zero.
/// \return Error code.
/// \retval matrixOk No error. Element was modified.
/// \retval matrixErrBadArg \p matrix was NULL.
/// \retval matrixErrBadIndex \p row or \p col was out of bounds.

matrix_err_t matrix_set_element(matrix_t *matrix, int row, int col, double value);



////////////////////////////////////////////////////////////////////////////////
/// Calculates the transpose of a matrix. x = a'
///
/// \brief Transpose matrix.
/// \param x Pointer to the result matrix that will be modified to hold the
/// result. This may point to the same matrix as \p a.
/// \param a Pointer to the matrix to be transposed.
/// \return Error code.
/// \retval matrixOk No error. Element was modified.
/// \retval matrixErrBadArg \p a or \p r was NULL.
/// \retval matrixErrMemAllocation A memory allocation error occured.

matrix_err_t matrix_transpose(matrix_t *x, const matrix_t *a);



////////////////////////////////////////////////////////////////////////////////
/// Calculates the multiplication of one matrix with another. x = a x b
///
/// \brief Multiplies two matrices.
/// \param x Pointer to the result matrix that will be modified to hold the
/// result of the multiplication. This may point to the same matrix as \p a or \p b.
/// \param a Pointer to the matrix that is the left hand side of the multiplication.
/// \param b Pointer to the matrix that is the right hand side of the multiplication.
/// \return Error code.
/// \retval matrixOk No error. Element was modified.
/// \retval matrixErrBadArg \p a, \p b or \p x was NULL.
/// \retval matrixErrBadIndex The number of columns of \p a does not match the number of rows
/// of \p b.
/// \retval matrixErrMemAllocation Memory allocation error.

matrix_err_t matrix_multiply(matrix_t *x, const matrix_t *a, const matrix_t *b);



////////////////////////////////////////////////////////////////////////////////
/// Calculates new matrix by removing a specified row and column from some other
/// matrix element.
///
/// \brief Matrix obtained by removing row and column of argument.
/// \param x Pointer to the result matrix. This may point to the same matrix as \p a.
/// \param a Pointer to the matrix whose row and column is to be removed for the
/// result.
/// \return Error code.
/// \retval matrixOk No error.
/// \retval matrixErrBadArg \p a or \p x was NULL.
/// \retval matrixErrBadIndex \p row or \p col was out of bounds.
/// \retval matrixErrMemAllocation Memory allocation error.

matrix_err_t matrix_remove_row_col(matrix_t *x, const matrix_t *a, int row, int col);



////////////////////////////////////////////////////////////////////////////////
/// Calculates the cofactor (often called the adjunct) of an element of a matrix.
/// It is the determinant of the matrix obtained by removing the row and column
/// corresponding to that element multiplied by (-1)^(row+col).
///
/// \brief Cofactor (adjunct) of a matrix element.
/// \param m Pointer to the matrix whose cofactor is to be calculated.
/// \param row Row of element in \p m whose cofactor is to be calculated.
/// \param col Column of element in \p m whose cofactor is to be calculated.
/// \param err Optional pointer to error code. Will be set to matrixOk if no
/// error. Set to matrixErrBadArg if \p m is not a valid matrix,
/// matrixErrNotSquare if \p m is not square, matrixErrBadIndex if \p row or
/// \p col is out of bounds, matrixErrMemAllocation on memory allocation
/// error. \p err may be NULL.
/// \return Cofactor of element at \p row, \p col of matrix \p m. 0 on error.

double matrix_cofactor(const matrix_t *m, int row, int col, matrix_err_t *err);



////////////////////////////////////////////////////////////////////////////////
/// Calculates the determinant of a matrix.
///
/// \brief Determinant of a matrix.
/// \param m Pointer to the matrix whose determinant is to be calculated.
/// \param err Optional pointer to error code. Will be set to matrixOk if no
/// error. Set to matrixErrBadArg if \p m is not a valid matrix,
/// matrixErrNotSquare if \p m is not square, matrixErrBadIndex if \p row or
/// \p col is out of bounds, matrixErrMemAllocation on memory allocation
/// error. \p err may be NULL.
/// \return Determinant matrix \p m. 0 on error.

double matrix_determinant(const matrix_t *m, matrix_err_t *err);



////////////////////////////////////////////////////////////////////////////////
/// Calculates the inversion of a matrix such that the matrix multiplied by its
/// inversion is equal to the identity matrix. The matrix must be square.
/// x = a^-1
///
/// \brief Invert a square matrix.
/// \param x Pointer to the result matrix. May be same as \p a.
/// \param a Pointer to the matrix that is to be inverted.
/// \return Error code
/// \retval matrixOk No error occured. Matrix was successfully inverted.
/// \retval matrixErrBadArg Either \p x or \p a is NULL or \p a has no rows or
/// columns.
/// \retval matrixErrNotSquare The matrix \p a is not square.
/// \retval matrixErrSingular The matrix \p a is singular and therefore has no
/// inverse.

matrix_err_t matrix_invert(matrix_t *x, const matrix_t *a);



////////////////////////////////////////////////////////////////////////////////
/// Swaps two rows of a matrix. row1 <--> row2
///
/// \brief Apply "swap" elementary row operation.
/// \param m Pointer to the matrix to which the ERO is to be apllied.
/// \param row1 Index of first row to swap (0 based).
/// \param row2 Index of second row to swap (0 based).
/// \return Error code
/// \retval matrixOk No error occured. Rows were successfully swapped.
/// \retval matrixErrBadArg The matrix \p m is NULL or has no elements.
/// \retval matrixErrBadIndex \p row1 or \p row2 is out of bounds.

matrix_err_t matrix_ero_swap(matrix_t *m, int row1, int row2);



////////////////////////////////////////////////////////////////////////////////
/// Multiply each element of a matrix row by specified non-zero multiplier.
/// row * multiplier ---> row
///
/// \brief Apply "multiply" elementary row operation.
/// \param m Pointer to the matrix to which the ERO is to be apllied.
/// \param row Index of row to be multiplied (0 based).
/// \param multiplier Value that the specified row is multiplied by. \p multiplier
/// may not be zero.
/// \return Error code
/// \retval matrixOk No error occured. Row was successfully multiplied.
/// \retval matrixErrBadArg The matrix \p m is NULL or has no elements.
/// \retval matrixErrBadIndex \p row is out of bounds.
/// \retval matrixErrZeroNotAllowed \p multiplier was zero. The multiplier is
/// deemed to be zero if it is within matrix_epsilon of zero.

matrix_err_t matrix_ero_multiply(matrix_t *m, int row, double multiplier);



////////////////////////////////////////////////////////////////////////////////
/// Add a non-zero multiple of one row to another.
/// row1 + row2 * multiplier ---> row1
///
/// \brief Apply "add multiple" elementary row operation.
/// \param m Pointer to the matrix to which the ERO is to be apllied.
/// \param row1 Index of target row for operation (0 based).
/// \param row2 Index of row to be added to \p row1 (0 based).
/// \param multiplier Multiple of \p row2 that is added to \p row1. \p multiplier
/// may not be zero.
/// \return Error code
/// \retval matrixOk No error occured. Row was successfully multiplied.
/// \retval matrixErrBadArg The matrix \p m is NULL or has no elements.
/// \retval matrixErrBadIndex \p row1 or \p row2 is out of bounds.
/// \retval matrixErrZeroNotAllowed \p multiplier was zero. The multiplier is
/// deemed to be zero if it is within matrix_epsilon of zero.

matrix_err_t matrix_ero_add_multiple(matrix_t *m, int row1, int row2, double multiplier);



#if defined(MATRIX_UNIT_TEST)

////////////////////////////////////////////////////////////////////////////////
/// Run matrix unit tests. Details of any failures will be sent to stdout.
///
/// \brief Test matrix functions.
/// \return Error count. Number of errors detected.
/// \retval 0 No error. All tests passed.

int matrix_test(void);

#endif


#endif //LIBTEST_MATRIX_H
